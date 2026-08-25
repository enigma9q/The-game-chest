package com.gamechest.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.gamechest.R
import com.gamechest.core.model.CarAvatar

object AndroidAssetProvider : AssetProvider {

    @Composable
    override fun getBoardPainter(isSheepGame: Boolean): Painter {
        val resId = if (isSheepGame) R.drawable.save_the_sheep_board else R.drawable.rev_up_racers_board
        return painterResource(id = resId)
    }

    @Composable
    override fun getTokenPainter(avatar: CarAvatar, isSheepGame: Boolean): Painter {
        val resId = if (isSheepGame) {
            when (avatar) {
                CarAvatar.SPEEDSTER_RED -> R.drawable.sheep_token_red
                CarAvatar.TURBO_BLUE -> R.drawable.sheep_token_blue
                CarAvatar.CYBER_YELLOW -> R.drawable.sheep_token_yellow
                CarAvatar.NITRO_GREEN -> R.drawable.sheep_token_green
                CarAvatar.APEX_PURPLE -> R.drawable.sheep_token_purple
                CarAvatar.MAGMA_ORANGE -> R.drawable.sheep_token_orange
                CarAvatar.NEON_CYAN -> R.drawable.sheep_token_cyan
                CarAvatar.HOT_PINK -> R.drawable.sheep_token_pink
            }
        } else {
            when (avatar) {
                CarAvatar.SPEEDSTER_RED -> R.drawable.car_token_red
                CarAvatar.TURBO_BLUE -> R.drawable.car_token_blue
                CarAvatar.CYBER_YELLOW -> R.drawable.car_token_yellow
                CarAvatar.NITRO_GREEN -> R.drawable.car_token_green
                CarAvatar.APEX_PURPLE -> R.drawable.car_token_purple
                CarAvatar.MAGMA_ORANGE -> R.drawable.car_token_orange
                CarAvatar.NEON_CYAN -> R.drawable.car_token_cyan
                CarAvatar.HOT_PINK -> R.drawable.car_token_pink
            }
        }
        return painterResource(id = resId)
    }
}
