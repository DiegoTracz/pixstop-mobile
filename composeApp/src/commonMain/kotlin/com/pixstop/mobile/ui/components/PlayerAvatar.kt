package com.pixstop.mobile.ui.components

import coil3.compose.AsyncImage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Retrato do jogador: moldura quadrada de borda ciano com as iniciais dentro e
 * o ponto verde de "online" no canto — o mesmo `PlayerAvatar` da web.
 *
 * Ainda mostra iniciais mesmo quando existe foto: exibir uma imagem remota
 * pede uma biblioteca de carregamento que o app ainda não tem.
 */
@Composable
fun PlayerAvatar(
    name: String,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    size: Dp = 40.dp,
    showOnlineIndicator: Boolean = true,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, PixColors.Cyan)
                .padding(2.dp)
                .background(PixColors.CyanAlpha20),
            contentAlignment = Alignment.Center,
        ) {
            // Com foto, é ela que aparece; as iniciais são o que sobra
            // quando ninguém escolheu retrato ainda.
            if (avatarUrl.isNullOrBlank()) {
                Text(
                    text = initialsOf(name),
                    style = PixTypography.badgeText,
                    color = PixColors.Cyan,
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(10.dp)
                    .background(PixColors.Dark)
                    .padding(2.dp)
                    .background(PixColors.Green),
            )
        }
    }
}

/**
 * Primeira letra do primeiro e do último nome, como na web.
 */
internal fun initialsOf(fullName: String): String {
    val parts = fullName.trim().split(' ').filter { it.isNotBlank() }

    return when (parts.size) {
        0 -> ""
        1 -> parts[0].take(1).uppercase()
        else -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
    }
}
