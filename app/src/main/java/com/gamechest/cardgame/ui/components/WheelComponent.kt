package com.gamechest.cardgame.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

data class WheelSegment(
    val id: Int,
    val value: Int,
    val label: String,
    val isBomb: Boolean = false,
    val colorHex: String,
    val secondaryHex: String
)

val WHEEL_SEGMENTS = listOf(
    WheelSegment(0, 2, "+2", false, "#F97316", "#C2410C"),   // 1. +2
    WheelSegment(1, 0, "0", false, "#10B981", "#047857"),    // 2. 0
    WheelSegment(2, 4, "+4", false, "#EF4444", "#B91C1C"),   // 3. +4
    WheelSegment(3, 2, "+2", false, "#F97316", "#C2410C"),   // 4. +2
    WheelSegment(4, 3, "+3", false, "#A855F7", "#7E22CE"),   // 5. +3
    WheelSegment(5, 6, "💣", true, "#DC2626", "#7F1D1D"),    // 6. 💣 BOMB (+6 cards)
    WheelSegment(6, 2, "+2", false, "#F97316", "#C2410C"),   // 7. +2
    WheelSegment(7, 0, "0", false, "#10B981", "#047857"),    // 8. 0
    WheelSegment(8, 3, "+3", false, "#A855F7", "#7E22CE"),   // 9. +3
    WheelSegment(9, 2, "+2", false, "#F97316", "#C2410C"),   // 10. +2
    WheelSegment(10, 4, "+4", false, "#EF4444", "#B91C1C")  // 11. +4
)

@Composable
fun WheelComponent(
    isSpinning: Boolean,
    targetResult: Int? = null,
    onSpinComplete: (Int) -> Unit,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 200.dp,
    enabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) }
    var currentResult by remember { mutableStateOf<Int?>(null) }

    fun calculateAngleForResult(resultVal: Int): Float {
        val matchingIndices = WHEEL_SEGMENTS.mapIndexedNotNull { idx, s -> if (s.value == resultVal) idx else null }
        val segIdx = matchingIndices.randomOrNull() ?: 0
        val segmentAngle = 360f / WHEEL_SEGMENTS.size
        val segCenter = segIdx * segmentAngle + (segmentAngle / 2f)
        val targetDeg = (270f - segCenter + 360f) % 360f
        return targetDeg
    }

    LaunchedEffect(isSpinning, targetResult) {
        if (isSpinning) {
            val randomSegment = WHEEL_SEGMENTS.random()
            val result = targetResult ?: randomSegment.value
            val baseAngle = calculateAngleForResult(result)
            val totalSpins = Random.nextInt(5, 8) * 360f
            val finalTarget = totalSpins + baseAngle + Random.nextFloat() * 10f - 5f // slight organic jitter

            rotationAnim.snapTo(0f)
            rotationAnim.animateTo(
                targetValue = finalTarget,
                animationSpec = tween(
                    durationMillis = 2800,
                    easing = FastOutSlowInEasing
                )
            )

            currentResult = result
            onSpinComplete(result)
        }
    }

    Box(
        modifier = modifier
            .size(wheelSize)
            .shadow(16.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 1. Rotating Wheel Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAnim.value)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f
            val segmentAngle = 360f / WHEEL_SEGMENTS.size

            WHEEL_SEGMENTS.forEachIndexed { index, segment ->
                val startAngle = index * segmentAngle
                val baseCol = parseHex(segment.colorHex)
                val secCol = parseHex(segment.secondaryHex)

                // Draw Segment Wedge
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(baseCol, secCol),
                        center = center,
                        radius = radius
                    ),
                    startAngle = startAngle,
                    sweepAngle = segmentAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )

                // Draw Segment Divider Lines
                val rad = Math.toRadians(startAngle.toDouble())
                val endX = center.x + radius * cos(rad).toFloat()
                val endY = center.y + radius * sin(rad).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 3f
                )
            }

            // Outer Neon Rim
            drawCircle(
                color = Color(0xFF0F172A),
                radius = radius,
                style = Stroke(width = 8f)
            )
            drawCircle(
                brush = Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color(0xFFFFD700), Color(0xFFFF007F), Color(0xFF00E5FF))),
                radius = radius - 4f,
                style = Stroke(width = 4f)
            )
        }

        // 2. Overlay Segment Numbers & Icons (11 segments)
        val segmentAngle = 360f / WHEEL_SEGMENTS.size
        WHEEL_SEGMENTS.forEachIndexed { index, segment ->
            val segCenterAngle = index * segmentAngle + (segmentAngle / 2f)
            val currentRot = (rotationAnim.value + segCenterAngle) % 360f
            val rad = Math.toRadians(currentRot.toDouble())
            val offsetDist = wheelSize.value * 0.33f
            val x = (offsetDist * cos(rad)).dp
            val y = (offsetDist * sin(rad)).dp

            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .clip(CircleShape)
                    .background(if (segment.isBomb) Color(0xFF7F1D1D) else Color(0xDD0F172A))
                    .border(1.2.dp, if (segment.isBomb) Color(0xFFEF4444) else Color.White, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = segment.label,
                    fontSize = (wheelSize.value * 0.075f).sp,
                    fontWeight = FontWeight.Black,
                    color = if (segment.isBomb) Color(0xFFFDE047) else parseHex(segment.colorHex)
                )
            }
        }

        // 3. Center Metallic Hub
        Surface(
            modifier = Modifier
                .size(wheelSize * 0.32f)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF00E5FF), CircleShape)
                .clickable(enabled = enabled && !isSpinning) {
                    if (enabled && !isSpinning) {
                        onSpinComplete(-1) // trigger spin start
                    }
                },
            shape = CircleShape,
            color = Color(0xFF0F172A),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF334155), Color(0xFF020617))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Spin Hub",
                        tint = if (enabled) Color(0xFF00E5FF) else Color.Gray,
                        modifier = Modifier.size(wheelSize * 0.14f)
                    )
                    Text(
                        text = if (isSpinning) "SPINNING" else "SPIN",
                        fontSize = (wheelSize.value * 0.055f).sp,
                        fontWeight = FontWeight.Black,
                        color = if (enabled) Color.White else Color.Gray
                    )
                }
            }
        }

        // 4. Indicator Pointer at Top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp)
        ) {
            Canvas(modifier = Modifier.size(24.dp, 28.dp)) {
                val path = Path().apply {
                    moveTo(size.width / 2f, size.height) // bottom tip
                    lineTo(0f, 0f) // top left
                    lineTo(size.width, 0f) // top right
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFFFDE047)
                )
                drawPath(
                    path = path,
                    color = Color(0xFF0F172A),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

private fun parseHex(hex: String): Color {
    val clean = hex.removePrefix("#")
    val colorLong = clean.toLong(16)
    return if (clean.length == 6) {
        Color(0xFF000000 or colorLong)
    } else {
        Color(colorLong)
    }
}
