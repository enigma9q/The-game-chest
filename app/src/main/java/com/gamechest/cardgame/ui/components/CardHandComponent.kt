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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
fun PlayerHandRow(
    player: WheelCardPlayer,
    isCurrentTurn: Boolean,
    isCardPlayable: (WheelCard) -> Boolean,
    onPlayCard: (WheelCard) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
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
                fontSize = 13.sp,
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

        // Horizontal Scrollable / Overlapping Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            player.hand.forEach { card ->
                val playable = isCurrentTurn && isCardPlayable(card)
                val isSelected = selectedCardId == card.id

                Box(
                    modifier = Modifier
                        .offset(y = if (isSelected) (-16).dp else if (playable) (-6).dp else 0.dp)
                        .padding(horizontal = 3.dp)
                ) {
                    PlayingCardView(
                        card = card,
                        isFaceUp = true,
                        isPlayable = playable,
                        isSelected = isSelected,
                        cardWidth = 76.dp,
                        cardHeight = 114.dp,
                        onClick = {
                            if (playable) {
                                onPlayCard(card)
                            } else {
                                selectedCardId = if (isSelected) null else card.id
                            }
                        }
                    )
                }
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
