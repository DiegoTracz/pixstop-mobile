package com.pixstop.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.Progression
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * A barra de XP.
 *
 * Diz três coisas, e só três: em que nível a pessoa está, quanto falta para
 * o próximo, e o que o próximo paga. É o que transforma "5% de cashback" em
 * algo que se acompanha — e é desenhada em blocos, não numa linha contínua,
 * porque nenhum canto do Pixstop é redondo.
 */
@Composable
fun XpBar(
    progression: Progression,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    segments: Int = 10,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Cyan)
            .background(PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LevelBadge(level = progression.level)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "PROGRESSO", style = PixTypography.badgeText, color = PixColors.Gray400)
                    Text(text = progression.displayTitle, style = PixTypography.sectionTitle, color = PixColors.Cyan)
                }
            }

            // A campanha manda mais que a sequência; e a sequência em risco
            // manda mais que a sequência tranquila — é hoje que ela se perde.
            val (badge, color) = when {
                progression.campaign != null -> "XP ${progression.campaign.multiplierLabel}" to PixColors.Yellow
                progression.streakAtRisk -> "Compre hoje: ${progression.streakDays} dias" to PixColors.Pink
                progression.hasStreak -> "${progression.streakDays} dias seguidos" to PixColors.Yellow
                else -> null to PixColors.Yellow
            }

            if (badge != null) {
                Text(
                    text = badge,
                    style = PixTypography.caption,
                    color = color,
                    modifier = Modifier.border(1.dp, color).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        SegmentedBar(fraction = progression.progressFraction, segments = segments)

        Text(
            text = when (val next = progression.nextLevel) {
                null -> "Nível máximo desta temporada."
                else -> "Faltam ${progression.xpToNext} XP para ${next.rewardPixels} pixels"
            },
            style = PixTypography.caption,
            color = PixColors.Gray300,
        )
    }
}

/**
 * Blocos que acendem conforme a fração. O último aceso pode ficar pela
 * metade — é o que mostra que a barra anda, e não só salta.
 */
@Composable
fun SegmentedBar(fraction: Float, segments: Int = 10, modifier: Modifier = Modifier) {
    // A animação vale a cada mudança: subir a barra é o momento inteiro.
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), animationSpec = tween(600))
    val filled = animated * segments

    Row(modifier = modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { index ->
            val portion = (filled - index).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(PixColors.Gray700),
            ) {
                if (portion > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(portion)
                            .background(PixColors.Cyan),
                    )
                }
            }
        }
    }
}

@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(36.dp)
            .height(36.dp)
            .background(PixColors.Cyan),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = level.toString(), style = PixTypography.sectionTitle, color = PixColors.Dark)
    }
}
