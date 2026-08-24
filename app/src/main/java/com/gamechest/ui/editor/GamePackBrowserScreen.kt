package com.gamechest.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamechest.core.loader.GamePackManager
import com.gamechest.core.model.GamePack
import com.gamechest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePackBrowserScreen(
    packManager: GamePackManager,
    onSelectPack: (GamePack) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val packs = remember { packManager.getAllPacks() }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Game Chest Catalog", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* File picker integration */ }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import .gamechest", tint = PrimaryNeon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Installed Game Packages",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Games are loaded dynamically from .gamechest bundle archives containing table layouts, rules, assets, and mutators.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            items(packs) { pack ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectPack(pack) },
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pack.manifest.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryNeon
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "v${pack.manifest.version}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = pack.manifest.description,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${pack.manifest.minPlayers}-${pack.manifest.maxPlayers} Players", fontSize = 12.sp, color = TextSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = NitroGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${pack.manifest.availableMutators.size} Mutators", fontSize = 12.sp, color = TextSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Casino, contentDescription = null, tint = TurboCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${pack.tableLayout.tilesCount} Tiles", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
