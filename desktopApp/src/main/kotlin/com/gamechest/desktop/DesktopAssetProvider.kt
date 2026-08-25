package com.gamechest.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.gamechest.core.model.CarAvatar
import com.gamechest.ui.components.AssetProvider

object DesktopAssetProvider : AssetProvider {

    @Composable
    override fun getBoardPainter(isSheepGame: Boolean): Painter {
        val fileName = if (isSheepGame) "save_the_sheep_board.jpg" else "rev_up_racers_board.jpg"
        return painterResource("drawable/$fileName")
    }

    @Composable
    override fun getTokenPainter(avatar: CarAvatar, isSheepGame: Boolean): Painter {
        val suffix = when (avatar) {
            CarAvatar.SPEEDSTER_RED -> "red"
            CarAvatar.TURBO_BLUE -> "blue"
            CarAvatar.CYBER_YELLOW -> "yellow"
            CarAvatar.NITRO_GREEN -> "green"
            CarAvatar.APEX_PURPLE -> "purple"
            CarAvatar.MAGMA_ORANGE -> "orange"
            CarAvatar.NEON_CYAN -> "cyan"
            CarAvatar.HOT_PINK -> "pink"
        }
        val prefix = if (isSheepGame) "sheep_token_" else "car_token_"
        return painterResource("drawable/$prefix$suffix.png")
    }
}
