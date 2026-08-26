package com.gamechest.cardgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.cardgame.engine.WheelCardPlayer
import com.gamechest.cardgame.model.WheelCard
import com.gamechest.ui.theme.*

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch

@Composable
fun PlayerHandRow(
    player: WheelCardPlayer,
    isCurrentTurn: Boolean,
    isCardPlayable: (WheelCard) -> Boolean,
    onPlayCard: (WheelCard) -> Unit,
    cardWidth: androidx.compose.ui.unit.Dp = 76.dp,
    cardHeight: androidx.compose.ui.unit.Dp = 114.dp,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player Info Ribbon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            val color = parseHexColor(player.profile.carAvatar.colorHex)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Text(
                text = "${player.profile.name} (Your Hand: ${player.hand.size} cards)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (isCurrentTurn) PrimaryNeon else TextSecondary
            )
            if (player.hand.size == 1) {
                Surface(
                    color = Color(0xFFEF4444),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "LAST CARD!",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Horizontal Scrollable Cards with Left & Right Arrow Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left Scroll Button
            IconButton(
                onClick = {
                    scope.launch {
                        val prevIndex = (listState.firstVisibleItemIndex - 3).coerceAtLeast(0)
                        listState.animateScrollToItem(prevIndex)
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceDarkCard.copy(alpha = 0.9f))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Scroll Left", tint = Color.White, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // LazyRow with items and auto-spacing
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(player.hand, key = { _, card -> card.id }) { _, card ->
                        val playable = isCurrentTurn && isCardPlayable(card)

                        Box(
                            modifier = Modifier
                                .offset(y = if (playable) (-8).dp else 0.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            PlayingCardView(
                                card = card,
                                isFaceUp = true,
                                isPlayable = playable,
                                isSelected = false,
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                onClick = {
                                    if (playable) {
                                        onPlayCard(card)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right Scroll Button
            IconButton(
                onClick = {
                    scope.launch {
                        val nextIndex = (listState.firstVisibleItemIndex + 3).coerceAtMost((player.hand.size - 1).coerceAtLeast(0))
                        listState.animateScrollToItem(nextIndex)
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceDarkCard.copy(alpha = 0.9f))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Scroll Right", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
fun OpponentHandBadge(
    player: WheelCardPlayer,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val playerColor = parseHexColor(player.profile.carAvatar.colorHex)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isCurrentTurn) 2.dp else 1.dp,
                color = if (isCurrentTurn) PrimaryNeon else BorderDark,
                shape = RoundedCornerShape(14.dp)
            ),
        color = SurfaceDarkCard,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(playerColor)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            }

            Column {
                Text(
                    text = player.profile.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentTurn) PrimaryNeon else TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${player.hand.size} cards",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            // Mini stacked card back icons
            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                repeat(minOf(player.hand.size, 4)) {
                    Box(
                        modifier = Modifier
                            .size(18.dp, 26.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF475569), RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
