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
    val value: Int,
    val label: String,
    val colorHex: String,
    val secondaryHex: String
)

val WHEEL_SEGMENTS = listOf(
    WheelSegment(0, "+0", "#10B981", "#047857"), // 0 - 72 deg: Emerald (+0 Cards - Lucky Escape!)
    WheelSegment(1, "+1", "#06B6D4", "#0E7490"), // 72 - 144 deg: Cyan (+1 Card)
    WheelSegment(2, "+2", "#F97316", "#C2410C"), // 144 - 216 deg: Orange (+2 Cards)
    WheelSegment(3, "+3", "#A855F7", "#7E22CE"), // 216 - 288 deg: Purple (+3 Cards)
    WheelSegment(4, "+4", "#EF4444", "#B91C1C")  // 288 - 360 deg: Crimson (+4 Cards!)
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

    // Pointer is at Top (270 degrees in standard polar coords)
    // Segment size = 72 degrees (360 / 5)
    // Segment 0 center is at 36 deg, Seg 1 is 108 deg, Seg 2 is 180 deg, Seg 3 is 252 deg, Seg 4 is 324 deg
    fun calculateAngleForResult(resultVal: Int): Float {
        val segIdx = WHEEL_SEGMENTS.indexOfFirst { it.value == resultVal }.coerceAtLeast(0)
        val segCenter = segIdx * 72f + 36f
        // To place segCenter at Top (270 deg): rotation = 270 - segCenter
        val targetDeg = (270f - segCenter + 360f) % 360f
        return targetDeg
    }

    LaunchedEffect(isSpinning, targetResult) {
        if (isSpinning) {
            val result = targetResult ?: Random.nextInt(0, 5)
            val baseAngle = calculateAngleForResult(result)
            val totalSpins = Random.nextInt(5, 8) * 360f
            val finalTarget = totalSpins + baseAngle + Random.nextFloat() * 20f - 10f // slight organic jitter

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

        // 2. Overlay Segment Numbers (+0, +1, +2, +3, +4)
        WHEEL_SEGMENTS.forEachIndexed { index, segment ->
            val segCenterAngle = index * 72f + 36f
            val currentRot = (rotationAnim.value + segCenterAngle) % 360f
            val rad = Math.toRadians(currentRot.toDouble())
            val offsetDist = wheelSize.value * 0.32f
            val x = (offsetDist * cos(rad)).dp
            val y = (offsetDist * sin(rad)).dp

            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .clip(CircleShape)
                    .background(Color(0xDD0F172A))
                    .border(1.5.dp, Color.White, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = segment.label,
                    fontSize = (wheelSize.value * 0.085f).sp,
                    fontWeight = FontWeight.Black,
                    color = parseHex(segment.colorHex)
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
