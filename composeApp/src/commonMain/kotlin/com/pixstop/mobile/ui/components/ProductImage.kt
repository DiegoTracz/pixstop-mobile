package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pixstop.mobile.ui.theme.PixColors

/**
 * A foto do produto, com a inicial do nome no lugar enquanto ela não chega.
 *
 * A inicial fica até a foto carregar de fato — e volta a ser o que se vê se
 * o produto não tem foto ou o download falha. Por baixo de uma foto com fundo
 * transparente (a lata, a garrafa) ela não pode continuar desenhada, por isso
 * a troca é pelo estado do carregamento, e não por empilhar as duas.
 *
 * `Fit`, e não `Crop`: foto de produto cortada esconde justamente o rótulo.
 */
@Composable
fun ProductImage(
    name: String,
    imageUrl: String?,
    initialStyle: TextStyle,
    modifier: Modifier = Modifier,
    imagePadding: Dp = 4.dp,
) {
    var loaded by remember(imageUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier.background(PixColors.Gray800),
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) {
            Text(
                text = name.take(1).uppercase(),
                style = initialStyle,
                color = PixColors.Gray500,
            )
        }

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().padding(imagePadding),
                contentScale = ContentScale.Fit,
                onSuccess = { loaded = true },
                onError = { loaded = false },
            )
        }
    }
}
