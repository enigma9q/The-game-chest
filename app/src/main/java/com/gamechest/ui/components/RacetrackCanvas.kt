package com.gamechest.ui.components

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gamechest.R
import com.gamechest.core.model.*
import com.gamechest.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.atan2

@Composable
fun RacetrackCanvas(
    layout: TableLayoutConfig,
    players: List<PlayerRuntimeState>,
    activeMutators: Set<MutatorId>,
    onTileClicked: (TileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isReverseHazards = activeMutators.contains(MutatorId.REVERSE_HAZARD_OVERDRIVE)

    // Load background board artwork
    val boardBitmap = ImageBitmap.imageResource(id = R.drawable.rev_up_racers_board)

    // Load colorized top-down sports car token sprites
    val carBitmaps = mapOf(
        CarAvatar.SPEEDSTER_RED to ImageBitmap.imageResource(R.drawable.car_token_red),
        CarAvatar.TURBO_BLUE to ImageBitmap.imageResource(R.drawable.car_token_blue),
        CarAvatar.CYBER_YELLOW to ImageBitmap.imageResource(R.drawable.car_token_yellow),
        CarAvatar.NITRO_GREEN to ImageBitmap.imageResource(R.drawable.car_token_green),
        CarAvatar.APEX_PURPLE to ImageBitmap.imageResource(R.drawable.car_token_purple),
        CarAvatar.MAGMA_ORANGE to ImageBitmap.imageResource(R.drawable.car_token_orange),
        CarAvatar.NEON_CYAN to ImageBitmap.imageResource(R.drawable.car_token_cyan),
        CarAvatar.HOT_PINK to ImageBitmap.imageResource(R.drawable.car_token_pink)
    )

    // Text paint with shadow for transparent background numbers
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.BLACK)
        }
    }

    val playerBadgeTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    // Driving Car Animated State for Each Player
    val playerCarStates = remember { mutableStateMapOf<String, AnimatedCarDriver>() }

    // Synchronize driving animation when player tile positions update
    players.forEach { player ->
        val targetTile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
        val tile1 = layout.tiles.find { it.id == 1 }
        val tile0 = layout.tiles.find { it.id == 0 }

        // At START (Tile 0), calculate race start heading pointing towards Tile 1
        val initialStartAngle = if (tile0 != null && tile1 != null) {
            val rad = atan2((tile1.y - tile0.y).toDouble(), (tile1.x - tile0.x).toDouble())
            (Math.toDegrees(rad).toFloat() + 90f)
        } else 103f

        val carDriver = playerCarStates.getOrPut(player.profile.id) {
            AnimatedCarDriver(
                initialX = targetTile.x,
                initialY = targetTile.y,
                initialAngle = if (player.currentTileId == 0) initialStartAngle else 0f,
                lastTileId = player.currentTileId
            )
        }

        LaunchedEffect(player.currentTileId) {
            if (carDriver.lastTileId != player.currentTileId) {
                val startId = carDriver.lastTileId
                val endId = player.currentTileId
                carDriver.lastTileId = endId

                // Determine route waypoints (step-by-step or direct bridge/oil connection)
                val waypoints = calculateWaypoints(startId, endId, layout)

                var prevTile = layout.tiles.find { it.id == startId } ?: layout.tiles.first()

                // Drive car fluidly along the track path
                for (nextTile in waypoints) {
                    val targetX = nextTile.x
                    val targetY = nextTile.y
                    val currentX = carDriver.animX.value
                    val currentY = carDriver.animY.value

                    val dx = targetX - currentX
                    val dy = targetY - currentY

                    if (dx * dx + dy * dy > 0.000005f) {
                        val isConnectionMove = layout.connections.any { 
                            it.fromTileId == prevTile.id && it.toTileId == nextTile.id 
                        }

                        // Calculate heading angle in degrees (car sprite top is 0 deg -> +90 deg offset)
                        val rad = atan2(dy.toDouble(), dx.toDouble())
                        var deg = Math.toDegrees(rad).toFloat() + 90f
                        val curAngle = carDriver.animAngle.value
                        var diff = (deg - curAngle) % 360f
                        if (diff > 180f) diff -= 360f
                        if (diff < -180f) diff += 360f
                        val targetHeading = curAngle + diff

                        val rotateDuration = if (isConnectionMove) 140 else 90
                        val driveDuration = if (isConnectionMove) 280 else 130

                        // Rotate car to face the bridge/oil track or road heading
                        launch {
                            carDriver.animAngle.animateTo(
                                targetHeading,
                                animationSpec = tween(durationMillis = rotateDuration, easing = FastOutSlowInEasing)
                            )
                        }

                        // Drive car smoothly to the next position
                        carDriver.animX.animateTo(
                            targetX,
                            animationSpec = tween(durationMillis = driveDuration, easing = if (isConnectionMove) FastOutSlowInEasing else LinearEasing)
                        )
                        carDriver.animY.animateTo(
                            targetY,
                            animationSpec = tween(durationMillis = driveDuration, easing = if (isConnectionMove) FastOutSlowInEasing else LinearEasing)
                        )

                        // If completed a bridge/oil connection, smoothly orient towards the next road node
                        if (isConnectionMove) {
                            val nextRoadTile = layout.tiles.find { it.id == nextTile.id + 1 }
                            if (nextRoadTile != null) {
                                val roadDx = nextRoadTile.x - nextTile.x
                                val roadDy = nextRoadTile.y - nextTile.y
                                val roadRad = atan2(roadDy.toDouble(), roadDx.toDouble())
                                val roadDeg = Math.toDegrees(roadRad).toFloat() + 90f
                                var roadDiff = (roadDeg - carDriver.animAngle.value) % 360f
                                if (roadDiff > 180f) roadDiff -= 360f
                                if (roadDiff < -180f) roadDiff += 360f
                                carDriver.animAngle.animateTo(
                                    carDriver.animAngle.value + roadDiff,
                                    animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    }
                    prevTile = nextTile
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1024f / 850f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .border(2.5.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(layout) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val height = size.height
                        val clickedTile = layout.tiles.minByOrNull { tile ->
                            val tx = tile.x * width
                            val ty = tile.y * height
                            (tx - offset.x) * (tx - offset.x) + (ty - offset.y) * (ty - offset.y)
                        }
                        if (clickedTile != null) {
                            onTileClicked(clickedTile)
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw High-Resolution Rev-Up Racers Board Image
            drawImage(
                image = boardBitmap,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(canvasW.toInt(), canvasH.toInt())
            )

            // 2. Draw Turbo Bridges (Going UP / Shortcuts: from smaller to bigger)
            layout.connections.filter { it.type == ConnectionType.TURBO_RAMP }.forEach { bridge ->
                val from = layout.tiles.find { it.id == bridge.fromTileId }
                val to = layout.tiles.find { it.id == bridge.toTileId }
                if (from != null && to != null) {
                    val p1 = Offset(from.x * canvasW, from.y * canvasH)
                    val p2 = Offset(to.x * canvasW, to.y * canvasH)
                    val bridgeColor = if (isReverseHazards) HazardRed else TurboCyan

                    // Glow line & core trajectory line
                    drawLine(
                        color = bridgeColor.copy(alpha = 0.45f),
                        start = p1,
                        end = p2,
                        strokeWidth = 9f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bridgeColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(bridgeColor, radius = 7f, center = p2)
                    drawCircle(Color.White, radius = 3f, center = p2)
                }
            }

            // 3. Draw Oil Spills (Going DOWN / Hazards: from bigger to smaller)
            layout.connections.filter { it.type == ConnectionType.OIL_SLICK }.forEach { spill ->
                val from = layout.tiles.find { it.id == spill.fromTileId }
                val to = layout.tiles.find { it.id == spill.toTileId }
                if (from != null && to != null) {
                    val p1 = Offset(from.x * canvasW, from.y * canvasH)
                    val p2 = Offset(to.x * canvasW, to.y * canvasH)
                    val spillColor = if (isReverseHazards) NitroGreen else Color(0xFFFF2A85)

                    drawLine(
                        color = spillColor.copy(alpha = 0.4f),
                        start = p1,
                        end = p2,
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = spillColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                        cap = StrokeCap.Round
                    )
                    drawCircle(spillColor, radius = 6.5f, center = p2)
                }
            }

            // 4. Draw Tile Node Numbers with TRANSPARENT BACKGROUND, perfectly centered
            val baseRadius = (canvasW * 0.024f).coerceIn(12f, 24f)
            textPaint.textSize = baseRadius * 1.05f

            layout.tiles.forEach { tile ->
                val tx = tile.x * canvasW
                val ty = tile.y * canvasH
                val isStart = tile.type == TileType.START
                val isFinish = tile.type == TileType.FINISH

                // Draw centered numbers directly over artwork with transparent background
                drawIntoCanvas { canvas ->
                    val textBounds = android.graphics.Rect()
                    when {
                        isStart -> {
                            // Start circle already has artwork label; draw clean start badge
                            textPaint.textSize = baseRadius * 0.70f
                            textPaint.color = android.graphics.Color.WHITE
                            val label = "START"
                            textPaint.getTextBounds(label, 0, label.length, textBounds)
                            val textY = ty - textBounds.exactCenterY()
                            canvas.nativeCanvas.drawText(label, tx, textY, textPaint)
                            textPaint.textSize = baseRadius * 1.05f
                        }
                        isFinish -> {
                            textPaint.textSize = baseRadius * 0.70f
                            textPaint.color = android.graphics.Color.YELLOW
                            val label = "FINISH"
                            textPaint.getTextBounds(label, 0, label.length, textBounds)
                            val textY = ty - textBounds.exactCenterY()
                            canvas.nativeCanvas.drawText(label, tx, textY, textPaint)
                            textPaint.textSize = baseRadius * 1.05f
                        }
                        else -> {
                            val label = "${tile.index}"
                            textPaint.color = android.graphics.Color.WHITE
                            textPaint.getTextBounds(label, 0, label.length, textBounds)
                            val textY = ty - textBounds.exactCenterY()
                            canvas.nativeCanvas.drawText(label, tx, textY, textPaint)
                        }
                    }
                }
            }

            // 5. Draw Fluid Driving Top-Down Sports Cars
            val carWidth = (baseRadius * 1.55f).toInt()
            val carHeight = (carWidth * 2.08f).toInt()

            players.forEachIndexed { pIdx, player ->
                val carDriver = playerCarStates[player.profile.id]
                val carBitmap = carBitmaps[player.profile.carAvatar] ?: carBitmaps[CarAvatar.SPEEDSTER_RED]!!

                val cx = if (carDriver != null) carDriver.animX.value * canvasW else {
                    val tile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
                    tile.x * canvasW
                }
                val cy = if (carDriver != null) carDriver.animY.value * canvasH else {
                    val tile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
                    tile.y * canvasH
                }
                val carRotation = carDriver?.animAngle?.value ?: 0f

                // Stagger cars side-by-side or behind each other when on the same node (Never overlapping/under)
                val isMoving = carDriver?.animX?.isRunning == true || carDriver?.animY?.isRunning == true
                val (drawX, drawY) = if (!isMoving && players.count { it.currentTileId == player.currentTileId } > 1) {
                    val sameTilePlayers = players.filter { it.currentTileId == player.currentTileId }
                    val idxOnTile = sameTilePlayers.indexOf(player)
                    val totalOnTile = sameTilePlayers.size

                    // Compute grid parking offset next to / behind each other
                    val (offsetX, offsetY) = when (totalOnTile) {
                        2 -> {
                            // Side-by-side: left and right
                            if (idxOnTile == 0) Pair(-carWidth * 0.65f, 0f) else Pair(carWidth * 0.65f, 0f)
                        }
                        3 -> {
                            // Front 2 side-by-side, 1 behind
                            when (idxOnTile) {
                                0 -> Pair(-carWidth * 0.65f, -carHeight * 0.35f)
                                1 -> Pair(carWidth * 0.65f, -carHeight * 0.35f)
                                else -> Pair(0f, carHeight * 0.45f)
                            }
                        }
                        else -> {
                            // 2x2 Grid: 2 in front, 2 behind
                            val row = if (idxOnTile < 2) -1 else 1
                            val col = if (idxOnTile % 2 == 0) -1 else 1
                            Pair(col * carWidth * 0.65f, row * carHeight * 0.45f)
                        }
                    }
                    Pair(cx + offsetX, cy + offsetY)
                } else {
                    Pair(cx, cy)
                }

                // Draw Ground Shadow under Car
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = carWidth * 0.72f,
                    center = Offset(drawX, drawY + 2f)
                )

                // Draw Rotated Car Token Sprite
                withTransform({
                    translate(drawX, drawY)
                    rotate(carRotation, pivot = Offset.Zero)
                }) {
                    drawImage(
                        image = carBitmap,
                        dstOffset = IntOffset(-carWidth / 2, -carHeight / 2),
                        dstSize = IntSize(carWidth, carHeight)
                    )
                }

                // Draw Player Badge Indicator (P1, P2, etc.) Next to Car
                val badgeRadius = baseRadius * 0.55f
                val badgeCenter = Offset(drawX + carWidth * 0.62f, drawY - carHeight * 0.36f)
                val carThemeColor = Color(android.graphics.Color.parseColor(player.profile.carAvatar.colorHex))

                drawCircle(
                    color = carThemeColor,
                    radius = badgeRadius,
                    center = badgeCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = badgeRadius,
                    center = badgeCenter,
                    style = Stroke(width = 2f)
                )

                drawIntoCanvas { canvas ->
                    val playerNum = player.profile.name.filter { it.isDigit() }.ifEmpty { "${pIdx + 1}" }
                    playerBadgeTextPaint.textSize = badgeRadius * 1.25f
                    val yOff = (playerBadgeTextPaint.descent() + playerBadgeTextPaint.ascent()) / 2f
                    canvas.nativeCanvas.drawText("P$playerNum", badgeCenter.x, badgeCenter.y - yOff, playerBadgeTextPaint)
                }
            }
        }
    }
}

private class AnimatedCarDriver(
    initialX: Float,
    initialY: Float,
    initialAngle: Float = 0f,
    var lastTileId: Int = 0
) {
    val animX = Animatable(initialX)
    val animY = Animatable(initialY)
    val animAngle = Animatable(initialAngle)
}

/**
 * Computes waypoints along the racetrack for fluid car movement:
 * - If traversing a bridge or oil spill: moves along road to the launchpad, then directly straight through the connection.
 * - Otherwise: moves step-by-step from node to node.
 */
private fun calculateWaypoints(
    fromTileId: Int,
    toTileId: Int,
    layout: TableLayoutConfig
): List<TileNode> {
    if (fromTileId == toTileId) return emptyList()

    // 1. Direct connection
    val directConnection = layout.connections.find { it.fromTileId == fromTileId && it.toTileId == toTileId }
    if (directConnection != null) {
        val targetNode = layout.tiles.find { it.id == toTileId }
        return if (targetNode != null) listOf(targetNode) else emptyList()
    }

    // 2. Check if a bridge or oil spill was taken as part of this turn's move:
    // e.g. rolled from fromTileId (e.g. 4) to conn.fromTileId (e.g. 7), and then took bridge to toTileId (e.g. 29)
    // or rolled from fromTileId (e.g. 38) to conn.fromTileId (e.g. 41), and then took oil spill to toTileId (e.g. 22)
    val triggeredConnection = layout.connections.find { conn ->
        conn.toTileId == toTileId && (
            (conn.type == ConnectionType.TURBO_RAMP && conn.fromTileId > fromTileId && conn.fromTileId - fromTileId <= 12) ||
            (conn.type == ConnectionType.OIL_SLICK && conn.fromTileId > fromTileId && conn.fromTileId - fromTileId <= 12)
        )
    }

    if (triggeredConnection != null) {
        val waypoints = mutableListOf<TileNode>()
        // Step forward from current tile to launchpad
        for (stepId in (fromTileId + 1)..triggeredConnection.fromTileId) {
            val node = layout.tiles.find { it.id == stepId }
            if (node != null) waypoints.add(node)
        }
        // Launch directly straight across the bridge / oil spill to destination tile
        val destNode = layout.tiles.find { it.id == toTileId }
        if (destNode != null && waypoints.lastOrNull()?.id != toTileId) {
            waypoints.add(destNode)
        }
        return waypoints
    }

    // 3. Normal step-by-step forward or backward circuit movement
    val step = if (toTileId > fromTileId) 1 else -1
    val waypoints = mutableListOf<TileNode>()
    var curr = fromTileId + step
    while (true) {
        val node = layout.tiles.find { it.id == curr }
        if (node != null) {
            waypoints.add(node)
        }
        if (curr == toTileId) break
        curr += step
    }
    return waypoints
}
