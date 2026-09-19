package com.pixstop.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.ui.theme.PixTypography
import kotlinx.coroutines.delay

/**
 * A espera do app: o mascote em ciclo RGB e, se houver, a frase.
 *
 * O mascote só entra se a espera passar de [delayMillis]: espera curta com um
 * LED piscando é pior que sem nada. O espaço dele fica reservado desde o
 * começo, então nada pula quando ele aparece. Mesma regra do `PixelLoader.vue`.
 */
@Composable
fun PixelLoader(
    modifier: Modifier = Modifier,
    message: String? = null,
    size: Dp = 64.dp,
    delayMillis: Long = 400,
) {
    var visible by remember { mutableStateOf(delayMillis <= 0) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        visible = true
    }

    Column(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            if (visible) {
                PixelMascot(state = MascotState.Loading, size = size, decorative = message != null)
            }
        }

        message?.let {
            Text(text = it, style = PixTypography.bodyMuted, textAlign = TextAlign.Center)
        }
    }
}
