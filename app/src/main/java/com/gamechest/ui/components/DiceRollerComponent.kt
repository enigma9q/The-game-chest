package com.gamechest.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
    extraRollAwarded: Boolean = false,
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .scale(if (isCurrentPlayerTurn) pulseScale else 1f)
                .rotate(rollRotation)
                .size(86.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (extraRollAwarded) {
                            listOf(Color(0xFF064E3B), Color(0xFF0F172A))
                        } else if (diceSpec.sides >= 60) {
                            listOf(Color(0xFF1E1B4B), Color(0xFF4338CA))
                        } else {
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        }
                    )
                )
                .border(
                    width = if (isCurrentPlayerTurn) 3.dp else 1.5.dp,
                    brush = if (extraRollAwarded) {
                        Brush.linearGradient(listOf(NitroGreen, TurboCyan))
                    } else if (isCurrentPlayerTurn) {
                        Brush.linearGradient(listOf(PrimaryNeon, SecondaryOrange))
                    } else {
                        Brush.linearGradient(listOf(TextMuted, Color.Transparent))
                    },
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable(enabled = isCurrentPlayerTurn && !isRolling) {
                    onRollClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isRolling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    color = PrimaryNeon,
                    strokeWidth = 3.dp
                )
            } else if (extraRollAwarded) {
                // When 6 is rolled, 6 goes away and ROLL AGAIN! is visible on the dice
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ROLL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NitroGreen,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "AGAIN!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = NitroGreen,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = NitroGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (lastRoll != null) {
                Text(
                    text = "${lastRoll.total}",
                    fontSize = if (diceSpec.sides >= 60) 32.sp else 38.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        lastRoll.isMax -> NitroGreen
                        lastRoll.total > 49 && diceSpec.sides >= 60 -> HazardRed
                        else -> TextPrimary
                    }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ROLL",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isCurrentPlayerTurn) PrimaryNeon else TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isCurrentPlayerTurn) AccentYellow else TextMuted
                    )
                }
            }
        }
    }
}
