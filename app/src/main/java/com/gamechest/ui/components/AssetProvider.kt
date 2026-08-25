package com.gamechest.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import com.gamechest.core.model.CarAvatar

interface AssetProvider {
    @Composable
    fun getBoardPainter(isSheepGame: Boolean): Painter

    @Composable
    fun getTokenPainter(avatar: CarAvatar, isSheepGame: Boolean): Painter
}

val LocalAssetProvider = staticCompositionLocalOf<AssetProvider> {
    error("No AssetProvider provided in CompositionLocalProvider")
}
