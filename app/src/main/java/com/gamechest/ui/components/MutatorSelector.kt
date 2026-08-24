package com.gamechest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.core.model.MutatorConfig
import com.gamechest.core.model.MutatorId
import com.gamechest.ui.theme.*

@Composable
fun MutatorSelector(
    availableMutators: List<MutatorConfig>,
    selectedMutators: Set<MutatorId>,
    onMutatorToggled: (MutatorId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Race Mutators (5 Presets)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        availableMutators.forEach { mutator ->
            val isSelected = selectedMutators.contains(mutator.id)
            val icon: ImageVector = when (mutator.id) {
                MutatorId.CLASSIC_GRAND_PRIX -> Icons.Default.Speed
                MutatorId.NITRO_TARGET_1D60 -> Icons.Default.Bolt
                MutatorId.NITRO_ASSIST_1D60 -> Icons.Default.RocketLaunch
                MutatorId.REVERSE_HAZARD_OVERDRIVE -> Icons.Default.SwapCalls
                MutatorId.CUSTOM_GRID_DICE_LOADOUT -> Icons.Default.Casino
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) SurfaceDarkCard else SurfaceDark)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) PrimaryNeon else Color(0xFF334155),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onMutatorToggled(mutator.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryNeon.copy(alpha = 0.2f) else Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mutator.name,
                        tint = if (isSelected) PrimaryNeon else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mutator.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                    Text(
                        text = mutator.description,
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isSelected,
                    onCheckedChange = { onMutatorToggled(mutator.id) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryNeon,
                        checkedTrackColor = PrimaryNeon.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }
        }
    }
}
