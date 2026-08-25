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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Dynamically load board artwork and token sprites via AssetProvider
    val isSheepGame = layout.backgroundImageAsset?.contains("sheep") == true
    val assetProvider = LocalAssetProvider.current
    val boardPainter = assetProvider.getBoardPainter(isSheepGame)

    val tokenPainters = CarAvatar.entries.associateWith {
        assetProvider.getTokenPainter(it, isSheepGame)
    }

    val textMeasurer = rememberTextMeasurer()

    // Driving Car Animated State for Each Player
    val scope = rememberCoroutineScope()
    class PlayerCarAnim(
        val animX: Animatable<Float, AnimationVector1D>,
        val animY: Animatable<Float, AnimationVector1D>,
        val animAngle: Animatable<Float, AnimationVector1D>
    )

    val playerCarStates = remember { mutableStateMapOf<String, PlayerCarAnim>() }

    // Initialize/sync car animation states
    players.forEach { player ->
        if (!playerCarStates.containsKey(player.profile.id)) {
            val startTile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
            playerCarStates[player.profile.id] = PlayerCarAnim(
                animX = Animatable(startTile.x),
                animY = Animatable(startTile.y),
                animAngle = Animatable(0f)
            )
        }
    }

    // Trigger smooth fluid driving animations when player moves
    players.forEach { player ->
        val carState = playerCarStates[player.profile.id]
        if (carState != null) {
            val targetTile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
            val currX = carState.animX.value
            val currY = carState.animY.value

            if (currX != targetTile.x || currY != targetTile.y) {
                LaunchedEffect(player.currentTileId) {
                    val dx = targetTile.x - currX
                    val dy = targetTile.y - currY
                    val targetAngle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI).toFloat() + 90f

                    // 1. Fluid steer turn towards target
                    carState.animAngle.animateTo(
                        targetValue = targetAngle,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )

                    // 2. Drive smoothly along path to destination
                    launch {
                        carState.animX.animateTo(
                            targetValue = targetTile.x,
                            animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                        )
                    }
                    launch {
                        carState.animY.animateTo(
                            targetValue = targetTile.y,
                            animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                        )
                    }

                    // 3. Align slightly towards track flow when parked
                    val nextTile = layout.tiles.getOrNull(targetTile.index + 1)
                    if (nextTile != null) {
                        val flowDx = nextTile.x - targetTile.x
                        val flowDy = nextTile.y - targetTile.y
                        val flowAngle = (atan2(flowDy.toDouble(), flowDx.toDouble()) * 180.0 / Math.PI).toFloat() + 90f
                        carState.animAngle.animateTo(
                            targetValue = flowAngle,
                            animationSpec = tween(durationMillis = 150, easing = LinearEasing)
                        )
                    }
                }
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

                val cx = if (carDriver != null) carDriver.animX.value * canvasW else {
                    val tile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
                    tile.x * canvasW
                }
                val cy = if (carDriver != null) carDriver.animY.value * canvasH else {
                    val tile = layout.tiles.find { it.id == player.currentTileId } ?: layout.tiles.first()
                    tile.y * canvasH
                }
                val tokenRotation = carDriver?.animAngle?.value ?: 0f

                // Stagger pieces side-by-side or behind each other when on the same node (Never overlapping)
                val isMoving = carDriver?.animX?.isRunning == true || carDriver?.animY?.isRunning == true
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

                // Draw Rotated Game Piece Token Sprite
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
