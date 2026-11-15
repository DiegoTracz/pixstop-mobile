package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.Achievement
import com.pixstop.mobile.domain.model.Challenge
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * O placar do desafio coletivo (docs/plans/CONQUISTAS.md no servidor).
 *
 * O placar é de todos; o número de cada um só a própria pessoa vê. Na Home
 * vai a versão curta, que abre "Meu progresso"; lá vão as cotas e a roda.
 */
@Composable
fun ChallengeBoard(
    challenge: Challenge,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Yellow)
            .background(PixColors.Darker)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "DESAFIO", style = PixTypography.badgeText, color = PixColors.Gray400)
                Text(text = challenge.name, style = PixTypography.sectionTitle, color = PixColors.Yellow)
            }

            Text(
                text = "${challenge.unitsTotal}/${challenge.goal}",
                style = PixTypography.sectionTitle,
                color = PixColors.Gray100,
            )
        }

        SegmentedBar(fraction = challenge.fraction, segments = challenge.quotas.size.coerceIn(4, 10) * 2)

        Text(text = challenge.headline, style = PixTypography.caption, color = PixColors.Gray100)

        challenge.nextQuota?.takeIf { !challenge.hasEnded }?.let { next ->
            Text(
                text = if (next.inStretch) "Você já está nessa cota." else "Uma compra garante a sua parte.",
                style = PixTypography.caption,
                color = if (next.inStretch) PixColors.Green else PixColors.Gray400,
            )
        }

        if (!challenge.xpEnabled) {
            Text(
                text = "A progressão por XP está desligada: o placar conta, mas as cotas não rendem XP agora.",
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )
        }
    }
}

/** As cotas, uma por linha, com a que a pessoa ganhou acesa. */
@Composable
fun ChallengeQuotas(challenge: Challenge, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        challenge.quotas.forEach { quota ->
            val color = when {
                quota.earned -> PixColors.Green
                quota.reached -> PixColors.Cyan
                else -> PixColors.Gray700
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, color)
                    .background(if (quota.earned) PixColors.GreenAlpha20 else PixColors.Darker)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${quota.label} ${challenge.unitLabel}",
                    color = if (quota.reached) PixColors.Gray100 else PixColors.Gray300,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (quota.earned) "+${quota.rewardXp} XP" else "${quota.rewardXp} XP",
                    style = PixTypography.sectionTitle,
                    color = if (quota.earned) PixColors.Green else PixColors.Gray400,
                )
            }
        }
    }
}

/** Quem está na roda: só nomes, nunca quantidade. */
@Composable
fun ChallengeParticipants(
    challenge: Challenge,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Gray700)
            .background(PixColors.Darker)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val names = challenge.participantNames.joinToString(", ") { if (it.isMe) "${it.name} (você)" else it.name }
        val more = if (challenge.moreParticipants > 0) " e mais ${challenge.moreParticipants}" else ""

        Text(
            text = if (names.isEmpty() && more.isEmpty()) "Ninguém ainda. A primeira compra abre a roda." else names + more,
            color = PixColors.Gray100,
        )

        if (challenge.hasJoined) {
            Text(
                text = "Você ajudou com ${challenge.myUnits} ${challenge.unitLabel}. Só você vê esse número.",
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )
        }

        Text(
            text = if (challenge.isHidden) "Voltar para a lista" else "Sair da lista",
            style = PixTypography.caption,
            color = PixColors.Cyan,
            modifier = Modifier.clickable(onClick = onToggleVisibility).padding(vertical = 4.dp),
        )
    }
}

@Composable
fun AchievementRow(achievement: Achievement, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Yellow)
            .background(PixColors.Darker)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = achievement.title, color = PixColors.Yellow)
            Text(text = achievement.description, style = PixTypography.caption, color = PixColors.Gray300)
            achievement.challenge?.let {
                Text(text = it, style = PixTypography.caption, color = PixColors.Gray400)
            }
        }

        if (achievement.xp > 0) {
            Text(text = "+${achievement.xp} XP", style = PixTypography.sectionTitle, color = PixColors.Cyan)
        }
    }
}
