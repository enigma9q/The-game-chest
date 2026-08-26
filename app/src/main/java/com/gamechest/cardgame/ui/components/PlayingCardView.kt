package com.gamechest.cardgame.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.cardgame.model.CardColor
import com.gamechest.cardgame.model.CardType
import com.gamechest.cardgame.model.WheelCard
import com.gamechest.ui.theme.parseHexColor

@Composable
fun PlayingCardView(
    card: WheelCard?,
    isFaceUp: Boolean = true,
    isPlayable: Boolean = false,
    isSelected: Boolean = false,
    cardWidth: Dp = 80.dp,
    cardHeight: Dp = 118.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playable_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val cardShape = RoundedCornerShape(12.dp)

    if (!isFaceUp || card == null) {
        // Face-down card back
        Surface(
            modifier = modifier
                .size(cardWidth, cardHeight)
                .shadow(6.dp, cardShape)
                .clip(cardShape)
                .border(2.dp, Color(0xFF475569), cardShape)
                .clickable(enabled = onClick != null) { onClick?.invoke() },
            shape = cardShape,
            color = Color(0xFF0F172A)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF020617))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(cardWidth * 0.6f)
                        .clip(CircleShape)
                        .background(Color(0x2200E5FF))
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Casino,
                        contentDescription = "Card Back",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(cardWidth * 0.35f)
                    )
                }
            }
        }
        return
    }

    // Face-up card
    val primaryColor = parseHexColor(card.color.hexColor)
    val secondaryColor = parseHexColor(card.color.secondaryHex)

    val backgroundBrush = if (card.color == CardColor.WILD) {
        Brush.sweepGradient(
            listOf(
                Color(0xFFEF4444),
                Color(0xFFF59E0B),
                Color(0xFF10B981),
                Color(0xFF3B82F6),
                Color(0xFFEF4444)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(primaryColor, secondaryColor)
        )
    }

    val elevation = if (isSelected) 14.dp else if (isPlayable) 8.dp else 4.dp
    val scale = if (isSelected) 1.08f else 1.0f

    Surface(
        modifier = modifier
            .size(cardWidth, cardHeight)
            .scale(scale)
            .shadow(elevation, cardShape)
            .clip(cardShape)
            .border(
                width = if (isPlayable) 3.dp else 1.5.dp,
                color = if (isPlayable) Color(0xFF00E5FF).copy(alpha = pulseGlow) else Color.White.copy(alpha = 0.3f),
                shape = cardShape
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = cardShape,
        color = Color(0xFF0F172A)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(4.dp)
        ) {
            // White inner pill container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(8.dp))
            ) {
                // Top-Left Mini Label
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = getCardShortLabel(card),
                        fontSize = (cardWidth.value * 0.16f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Center Icon / Big Number
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        card.number != null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${card.number}",
                                    fontSize = (cardWidth.value * 0.46f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-1).sp
                                )
                                if (card.isSpinShield) {
                                    Surface(
                                        color = Color(0xDD1E1B4B),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "SHIELD",
                                            fontSize = (cardWidth.value * 0.12f).sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFDE047),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                        card.type == CardType.DIRECTION_REVERSE -> {
                            Icon(
                                Icons.Default.SwapCalls,
                                contentDescription = "Reverse",
                                tint = Color.White,
                                modifier = Modifier.size(cardWidth * 0.44f)
                            )
                        }
                        card.type == CardType.DOUBLE_PLAY -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = "Double Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(cardWidth * 0.36f)
                                )
                                Text(
                                    text = "+1 PLAY",
                                    fontSize = (cardWidth.value * 0.14f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        card.type == CardType.BASIC_SPIN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Spin",
                                    tint = Color.White,
                                    modifier = Modifier.size(cardWidth * 0.38f)
                                )
                                Text(
                                    text = "SPIN",
                                    fontSize = (cardWidth.value * 0.15f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        card.type == CardType.SUPER_SPIN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.RotateRight,
                                    contentDescription = "Super Spin",
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(cardWidth * 0.38f)
                                )
                                Text(
                                    text = "SUPER\n+2 SPIN",
                                    fontSize = (cardWidth.value * 0.13f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFDE047),
                                    textAlign = TextAlign.Center,
                                    lineHeight = (cardWidth.value * 0.14f).sp
                                )
                            }
                        }
                        card.type == CardType.COLOR_CHOICE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Color Choice",
                                    tint = Color.White,
                                    modifier = Modifier.size(cardWidth * 0.38f)
                                )
                                Text(
                                    text = "COLOR",
                                    fontSize = (cardWidth.value * 0.14f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        card.type == CardType.BET_AND_SPIN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MonetizationOn,
                                    contentDescription = "Bet & Spin",
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(cardWidth * 0.38f)
                                )
                                Text(
                                    text = "BET &\nSPIN",
                                    fontSize = (cardWidth.value * 0.13f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (cardWidth.value * 0.14f).sp
                                )
                            }
                        }
                        card.type == CardType.ALL_SPIN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Groups,
                                    contentDescription = "All Spin",
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(cardWidth * 0.38f)
                                )
                                Text(
                                    text = "ALL\nSPIN",
                                    fontSize = (cardWidth.value * 0.13f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (cardWidth.value * 0.14f).sp
                                )
                            }
                        }
                    }
                }

                // Bottom-Right Mini Label
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = getCardShortLabel(card),
                        fontSize = (cardWidth.value * 0.16f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun getCardShortLabel(card: WheelCard): String {
    return when (card.type) {
        CardType.NUM_1 -> "1"
        CardType.NUM_2 -> "2"
        CardType.NUM_3 -> "3"
        CardType.NUM_4 -> "4"
        CardType.NUM_5 -> "5"
        CardType.NUM_6 -> "6"
        CardType.NUM_7 -> "7🛡️"
        CardType.NUM_8 -> "8"
        CardType.NUM_9 -> "9"
        CardType.NUM_10 -> "10"
        CardType.DIRECTION_REVERSE -> "⇄"
        CardType.DOUBLE_PLAY -> "2x"
        CardType.BASIC_SPIN -> "🎡"
        CardType.SUPER_SPIN -> "★🎡"
        CardType.COLOR_CHOICE -> "🎨"
        CardType.BET_AND_SPIN -> "🎯"
        CardType.ALL_SPIN -> "👥"
    }
}
