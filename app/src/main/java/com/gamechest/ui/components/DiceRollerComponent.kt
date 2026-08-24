package com.gamechest.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.core.model.DiceRollResult
import com.gamechest.core.model.DiceSpec
import com.gamechest.ui.theme.*

@Composable
fun DiceRollerComponent(
    diceSpec: DiceSpec,
    lastRoll: DiceRollResult?,
    isRolling: Boolean,
    isCurrentPlayerTurn: Boolean,
    onRollClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrentPlayerTurn && !isRolling) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rollRotation by animateFloatAsState(
        targetValue = if (isRolling) 720f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "roll_rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .scale(if (isCurrentPlayerTurn) pulseScale else 1f)
                .rotate(rollRotation)
                .size(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (diceSpec.sides >= 60) {
                            listOf(Color(0xFF1E1B4B), Color(0xFF4338CA))
                        } else {
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        }
                    )
                )
                .border(
                    width = if (isCurrentPlayerTurn) 3.dp else 1.5.dp,
                    brush = if (isCurrentPlayerTurn) {
                        Brush.linearGradient(listOf(PrimaryNeon, SecondaryOrange))
                    } else {
                        Brush.linearGradient(listOf(TextMuted, Color.Transparent))
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(enabled = isCurrentPlayerTurn && !isRolling) {
                    onRollClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isRolling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = PrimaryNeon,
                    strokeWidth = 3.dp
                )
            } else if (lastRoll != null) {
                Text(
                    text = "${lastRoll.total}",
                    fontSize = if (diceSpec.sides >= 60) 32.sp else 38.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        lastRoll.isMax -> NitroGreen
                        lastRoll.total > 50 && diceSpec.sides >= 60 -> HazardRed
                        else -> TextPrimary
                    }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (diceSpec.sides >= 60) Icons.Default.Speed else Icons.Default.Casino,
                        contentDescription = "Roll",
                        tint = if (isCurrentPlayerTurn) PrimaryNeon else TextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "ROLL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentPlayerTurn) PrimaryNeon else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = diceSpec.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCurrentPlayerTurn) AccentYellow else TextSecondary
        )
    }
}
