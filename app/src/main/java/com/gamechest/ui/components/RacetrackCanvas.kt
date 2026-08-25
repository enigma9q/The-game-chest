package com.gamechest.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.core.model.*
import com.gamechest.ui.theme.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sign

private data class TrackPoint(
    val x: Float,
    val y: Float,
    val isJump: Boolean = false,
    val isSpin: Boolean = false
)

private class PlayerCarAnim(
    initialX: Float,
    initialY: Float,
    initialAngle: Float
) {
    var posX by mutableFloatStateOf(initialX)
    var posY by mutableFloatStateOf(initialY)
    var angle by mutableFloatStateOf(initialAngle)
    var isMoving by mutableStateOf(false)
}

@Composable
fun RacetrackCanvas(
    layout: TableLayoutConfig,
    players: List<PlayerRuntimeState>,
    activeMutators: Set<MutatorId>,
    onTileClicked: (TileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamically load board artwork and token sprites via AssetProvider
    val isSheepGame = layout.backgroundImageAsset?.contains("sheep") == true
    val isNitroMutator = activeMutators.contains(MutatorId.NITRO_TARGET_1D60) ||
            activeMutators.contains(MutatorId.NITRO_ASSIST_1D60)

    val assetProvider = LocalAssetProvider.current
    val boardPainter = assetProvider.getBoardPainter(isSheepGame)

    val tokenPainters = CarAvatar.entries.associateWith {
        assetProvider.getTokenPainter(it, isSheepGame)
    }

    val textMeasurer = rememberTextMeasurer()

    val playerCarStates = remember { mutableStateMapOf<String, PlayerCarAnim>() }
    val lastSettledTileMap = remember { mutableStateMapOf<String, Int>() }

    // Initialize/sync car animation states (Start line faces EAST / 90 degrees)
    players.forEach { player ->
        if (!playerCarStates.containsKey(player.profile.id)) {
            val startTile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
            playerCarStates[player.profile.id] = PlayerCarAnim(
                initialX = startTile.x,
                initialY = startTile.y,
                initialAngle = if (player.currentTileId == 0) 90f else 0f
            )
            lastSettledTileMap[player.profile.id] = player.currentTileId
        }
    }

    // Continuous Fluid Road Driving Animation along the Track Path
    players.forEach { player ->
        val carState = playerCarStates[player.profile.id]
        val fromTileId = lastSettledTileMap[player.profile.id] ?: player.currentTileId
        val targetTileId = player.currentTileId

        if (carState != null && fromTileId != targetTileId) {
            LaunchedEffect(targetTileId) {
                // 1. If restarting game to Start Line (tile 0), snap directly to start line facing EAST
                if (targetTileId == 0) {
                    val startTile = layout.tiles.find { it.id == 0 } ?: layout.tiles.first()
                    carState.posX = startTile.x
                    carState.posY = startTile.y
                    carState.angle = 90f
                    carState.isMoving = false
                    lastSettledTileMap[player.profile.id] = 0
                    return@LaunchedEffect
                }

                // 2. Build the continuous trajectory of waypoints:
                val points = mutableListOf<TrackPoint>()
                val startTile = layout.tiles.find { it.id == fromTileId } ?: layout.tiles.first()
                points.add(TrackPoint(startTile.x, startTile.y))

                // Check if this move was a Bridge/Ramp or Oil Spill shortcut/hazard
                // A connection ONLY triggers if the player's trajectory reached conn.fromTileId and landed on conn.toTileId
                val triggerConnection = layout.connections.find { conn ->
                    conn.toTileId == targetTileId && conn.fromTileId > fromTileId
                }

                val finishId = layout.finishTileId
                // Check if player bounced off finish line
                val isBounceBack = fromTileId < finishId && targetTileId < finishId && (fromTileId + (finishId - targetTileId) >= finishId) && triggerConnection == null

                if (triggerConnection != null) {
                    // Step A: Drive along road to connection entrance node
                    for (id in (fromTileId + 1)..triggerConnection.fromTileId) {
                        val t = layout.tiles.find { it.id == id }
                        if (t != null) points.add(TrackPoint(t.x, t.y))
                    }
                    // Step B: Bridge jump or Oil spin to destination node
                    val destTile = layout.tiles.find { it.id == triggerConnection.toTileId }
                    if (destTile != null) {
                        if (triggerConnection.type == ConnectionType.TURBO_RAMP) {
                            points.add(TrackPoint(destTile.x, destTile.y, isJump = true))
                        } else {
                            points.add(TrackPoint(destTile.x, destTile.y, isSpin = true))
                        }
                    }
                } else if (isBounceBack) {
                    // Follow road forward to Finish Line
                    for (id in (fromTileId + 1)..finishId) {
                        val t = layout.tiles.find { it.id == id }
                        if (t != null) points.add(TrackPoint(t.x, t.y))
                    }
                    // Follow road backward from Finish Line to destination tile
                    for (id in (finishId - 1) downTo targetTileId) {
                        val t = layout.tiles.find { it.id == id }
                        if (t != null) points.add(TrackPoint(t.x, t.y))
                    }
                } else {
                    // Standard step-by-step road traversal (e.g. landing on 4, 5, 22 stays on 4, 5, 22)
                    val stepIds = if (fromTileId < targetTileId) {
                        (fromTileId + 1..targetTileId).toList()
                    } else {
                        (fromTileId - 1 downTo targetTileId).toList()
                    }

                    stepIds.forEach { id ->
                        val t = layout.tiles.find { it.id == id }
                        if (t != null) points.add(TrackPoint(t.x, t.y))
                    }
                }

                if (points.size < 2) {
                    lastSettledTileMap[player.profile.id] = targetTileId
                    return@LaunchedEffect
                }

                // 3. Calculate segment lengths and cumulative distances for smooth spline motion
                val segLengths = FloatArray(points.size - 1)
                var totalDist = 0f
                for (i in 0 until points.size - 1) {
                    val dx = points[i + 1].x - points[i].x
                    val dy = points[i + 1].y - points[i].y
                    val len = kotlin.math.sqrt(dx * dx + dy * dy)
                    segLengths[i] = len
                    totalDist += len
                }

                val totalNodes = points.size - 1
                val msPerNode = if (isNitroMutator) 65 else 160
                val totalDuration = (totalNodes * msPerNode).coerceIn(320, 3600)

                val progressAnim = Animatable(0f)
                var lastSteerAngle = carState.angle
                carState.isMoving = true

                // 4. Single continuous animation passing smoothly down the road
                progressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = totalDuration, easing = FastOutSlowInEasing)
                ) {
                    val currentDist = value * totalDist

                    // Find active segment
                    var accumulated = 0f
                    var segIdx = 0
                    while (segIdx < segLengths.size - 1 && accumulated + segLengths[segIdx] < currentDist) {
                        accumulated += segLengths[segIdx]
                        segIdx++
                    }

                    val segLen = segLengths[segIdx]
                    val segFraction = if (segLen > 0.0001f) ((currentDist - accumulated) / segLen).coerceIn(0f, 1f) else 1f

                    val pA = points[segIdx]
                    val pB = points[segIdx + 1]

                    val posX = pA.x + segFraction * (pB.x - pA.x)
                    val posY = pA.y + segFraction * (pB.y - pA.y)

                    // Continuous tangent direction
                    val dx = pB.x - pA.x
                    val dy = pB.y - pA.y
                    val segmentHeading = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI).toFloat() + 90f

                    var angleDiff = (segmentHeading - lastSteerAngle) % 360f
                    if (angleDiff > 180f) angleDiff -= 360f
                    if (angleDiff < -180f) angleDiff += 360f

                    // Corner Drift Math
                    val driftOffset = if (abs(angleDiff) > 20f && !isSheepGame) {
                        val intensity = if (isNitroMutator) 36f else 18f
                        sign(angleDiff) * intensity * (1f - abs(segFraction - 0.5f) * 2f)
                    } else 0f

                    val targetAngle = if (pB.isSpin) {
                        segmentHeading + segFraction * 360f
                    } else {
                        segmentHeading + driftOffset
                    }

                    // Smooth steering damping
                    val smoothedAngle = lastSteerAngle + (targetAngle - lastSteerAngle) * 0.40f
                    lastSteerAngle = smoothedAngle

                    carState.posX = posX
                    carState.posY = posY
                    carState.angle = smoothedAngle
                }

                carState.isMoving = false

                // 5. Align car smoothly to road flow when parked
                val finalTile = layout.tiles.find { it.id == targetTileId }
                val nextTile = finalTile?.let { layout.tiles.getOrNull(it.index + 1) }
                if (targetTileId == 0) {
                    carState.angle = 90f
                } else if (finalTile != null && nextTile != null) {
                    val flowDx = nextTile.x - finalTile.x
                    val flowDy = nextTile.y - finalTile.y
                    val flowAngle = (atan2(flowDy.toDouble(), flowDx.toDouble()) * 180.0 / Math.PI).toFloat() + 90f
                    carState.angle = flowAngle
                }

                lastSettledTileMap[player.profile.id] = targetTileId
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkBackground)
            .border(2.dp, BorderDark, RoundedCornerShape(20.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(layout) {
                    detectTapGestures { tapOffset ->
                        val canvasW = size.width
                        val canvasH = size.height
                        val touchRadius = canvasW * 0.045f

                        val clickedTile = layout.tiles.find { tile ->
                            val tx = tile.x * canvasW
                            val ty = tile.y * canvasH
                            val dist = kotlin.math.sqrt((tx - tapOffset.x) * (tx - tapOffset.x) + (ty - tapOffset.y) * (ty - tapOffset.y))
                            dist <= touchRadius
                        }

                        if (clickedTile != null) {
                            onTileClicked(clickedTile)
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val baseRadius = canvasW * 0.024f

            // 1. Draw High-Res Top-Down Board Background Artwork
            with(boardPainter) {
                draw(size = Size(canvasW, canvasH))
            }

            // 2. Draw Track Stepping Tiles (Subtle clean interactive glow on hover/active)
            layout.tiles.forEach { tile ->
                val tx = tile.x * canvasW
                val ty = tile.y * canvasH
                val isStart = tile.type == TileType.START
                val isFinish = tile.type == TileType.FINISH
                val radius = when {
                    isStart || isFinish -> baseRadius * 1.35f
                    else -> baseRadius * 0.95f
                }

                val hasPlayers = players.any { it.currentTileId == tile.id }
                if (hasPlayers) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = radius * 1.35f,
                        center = Offset(tx, ty)
                    )
                }

                // Draw tile label / index using Compose drawText
                val label = when {
                    isStart -> "START"
                    isFinish -> "HOME"
                    else -> "${tile.index}"
                }

                val textColor = when {
                    isStart -> Color(0xFF10B981)
                    isFinish -> Color(0xFFF59E0B)
                    else -> Color.White
                }

                val fontSize = if (isStart || isFinish) (baseRadius * 0.65f).sp else (baseRadius * 0.82f).sp

                val textResult = textMeasurer.measure(
                    text = AnnotatedString(label),
                    style = TextStyle(
                        color = textColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 4f)
                    )
                )

                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(tx - textResult.size.width / 2f, ty - textResult.size.height / 2f)
                )
            }

            // 3. Draw Fluid Driving Game Piece Tokens (Sheep or Sports Cars)
            val tokenWidth = (baseRadius * 1.55f).toInt()
            val tokenHeight = if (isSheepGame) tokenWidth else (tokenWidth * 2.08f).toInt()

            players.forEachIndexed { pIdx, player ->
                val carDriver = playerCarStates[player.profile.id]
                val tokenPainter = tokenPainters[player.profile.carAvatar] ?: tokenPainters[CarAvatar.SPEEDSTER_RED]!!

                val cx = if (carDriver != null) carDriver.posX * canvasW else {
                    val tile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
                    tile.x * canvasW
                }
                val cy = if (carDriver != null) carDriver.posY * canvasH else {
                    val tile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
                    tile.y * canvasH
                }
                val tokenRotation = carDriver?.angle ?: (if (player.currentTileId == 0) 90f else 0f)

                // Stagger pieces side-by-side or behind each other when on the same node (Never overlapping)
                val isMoving = carDriver?.isMoving == true
                val (drawX, drawY) = if (!isMoving && players.count { it.currentTileId == player.currentTileId } > 1) {
                    val sameTilePlayers = players.filter { it.currentTileId == player.currentTileId }
                    val idxOnTile = sameTilePlayers.indexOf(player)
                    val totalOnTile = sameTilePlayers.size

                    val (offsetX, offsetY) = when (totalOnTile) {
                        2 -> {
                            if (idxOnTile == 0) Pair(-tokenWidth * 0.65f, 0f) else Pair(tokenWidth * 0.65f, 0f)
                        }
                        3 -> {
                            when (idxOnTile) {
                                0 -> Pair(-tokenWidth * 0.65f, -tokenHeight * 0.35f)
                                1 -> Pair(tokenWidth * 0.65f, -tokenHeight * 0.35f)
                                else -> Pair(0f, tokenHeight * 0.45f)
                            }
                        }
                        else -> {
                            val row = if (idxOnTile < 2) -1 else 1
                            val col = if (idxOnTile % 2 == 0) -1 else 1
                            Pair(col * tokenWidth * 0.65f, row * tokenHeight * 0.45f)
                        }
                    }
                    Pair(cx + offsetX, cy + offsetY)
                } else {
                    Pair(cx, cy)
                }

                // Draw Ground Shadow under Game Piece
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = tokenWidth * 0.72f,
                    center = Offset(drawX, drawY + 2f)
                )

                // Draw Rotated Game Piece Token Sprite with Dynamic Drift Angle
                withTransform({
                    translate(drawX - tokenWidth / 2f, drawY - tokenHeight / 2f)
                    rotate(if (isSheepGame) 0f else tokenRotation, pivot = Offset(tokenWidth / 2f, tokenHeight / 2f))
                }) {
                    with(tokenPainter) {
                        draw(size = Size(tokenWidth.toFloat(), tokenHeight.toFloat()))
                    }
                }

                // Draw Player Badge Indicator (P1, P2, etc.) Next to Game Piece
                val badgeRadius = baseRadius * 0.55f
                val badgeCenter = Offset(drawX + tokenWidth * 0.62f, drawY - tokenHeight * 0.36f)
                val tokenThemeColor = parseHexColor(player.profile.carAvatar.colorHex)

                drawCircle(
                    color = tokenThemeColor,
                    radius = badgeRadius,
                    center = badgeCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = badgeRadius,
                    center = badgeCenter,
                    style = Stroke(width = 2f)
                )

                val playerNum = player.profile.name.filter { it.isDigit() }.ifEmpty { "${pIdx + 1}" }
                val badgeTextResult = textMeasurer.measure(
                    text = AnnotatedString("P$playerNum"),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = (badgeRadius * 0.95f).sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                )

                drawText(
                    textLayoutResult = badgeTextResult,
                    topLeft = Offset(badgeCenter.x - badgeTextResult.size.width / 2f, badgeCenter.y - badgeTextResult.size.height / 2f)
                )
            }
        }
    }
}
