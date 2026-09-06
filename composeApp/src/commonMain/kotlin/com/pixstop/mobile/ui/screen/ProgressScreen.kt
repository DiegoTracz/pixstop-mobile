package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.core.text.IsoInstant
import com.pixstop.mobile.domain.model.Progression
import com.pixstop.mobile.domain.model.ProgressionLevel
import com.pixstop.mobile.domain.model.ProgressionMission
import com.pixstop.mobile.domain.model.Ranking
import com.pixstop.mobile.domain.model.XpEntry
import com.pixstop.mobile.ui.components.LevelBadge
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.SegmentedBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ProgressViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Meu progresso": a barra por dentro.
 *
 * Os níveis todos, com os alcançados acesos, e o histórico de como o XP foi
 * ganho — dia a dia, regra a regra. É a resposta a "por que estou no nível 2
 * e não no 3", que a barra sozinha não dá.
 */
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore, state.hasNextPage) {
        if (shouldLoadMore && state.hasNextPage) {
            viewModel.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Meu progresso", onBack = onBack)

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PixColors.Cyan)
            }

            state.progression == null -> Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = state.error ?: "A progressão por XP ainda não está ligada nesta empresa.",
                    style = if (state.error != null) PixTypography.errorText else PixTypography.bodyMuted,
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val progression = state.progression!!

                item { Summary(progression) }

                if (progression.missions.isNotEmpty()) {
                    item {
                        Text(text = "Missões", style = PixTypography.sectionTitle, color = PixColors.Cyan)
                    }

                    items(progression.missions, key = { "mission-${it.code}" }) { mission ->
                        MissionRow(mission)
                    }
                }

                progression.ranking?.let { ranking ->
                    item {
                        Text(
                            text = "${ranking.department} · você está em ${ranking.position}º de ${ranking.members}",
                            style = PixTypography.sectionTitle,
                            color = PixColors.Cyan,
                        )
                    }

                    item { RankingCard(ranking) }
                }

                item {
                    Text(text = "Níveis", style = PixTypography.sectionTitle, color = PixColors.Cyan)
                }

                items(progression.levels, key = { "level-${it.level}" }) { level ->
                    LevelRow(level = level, current = level.level == progression.level)
                }

                if (state.entries.isNotEmpty()) {
                    item {
                        Text(
                            text = "Como ganhei",
                            style = PixTypography.sectionTitle,
                            color = PixColors.Cyan,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    items(state.entries, key = { it.id }) { entry ->
                        EntryRow(entry)
                    }
                }

                if (state.isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PixColors.Cyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(progression: Progression) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Cyan)
            .background(PixColors.Darker)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(level = progression.level)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = progression.displayTitle, style = PixTypography.pageTitle, color = PixColors.Cyan)
                Text(
                    text = "${progression.xpTotal} XP na temporada ${progression.season}" +
                        (progression.seasonEndsAt?.let { " · termina em ${it.take(10)}" } ?: ""),
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )
            }
        }

        SegmentedBar(fraction = progression.progressFraction)

        Text(
            text = when (val next = progression.nextLevel) {
                null -> "Você chegou ao nível máximo desta temporada."
                else -> "Faltam ${progression.xpToNext} XP para o nível ${next.level}" +
                    (next.title?.let { " — $it" } ?: "") + ", que paga ${next.rewardPixels} pixels."
            },
            style = PixTypography.bodySecondary,
            color = PixColors.Gray100,
        )

        if (progression.streakAtRisk) {
            Text(
                text = "Sua sequência de ${progression.streakDays} dias termina hoje se você não comprar.",
                style = PixTypography.caption,
                color = PixColors.Pink,
            )
        } else if (progression.hasStreak) {
            Text(
                text = "${progression.streakDays} dias seguidos comprando. Amanhã vale mais.",
                style = PixTypography.caption,
                color = PixColors.Yellow,
            )
        }

        progression.campaign?.let { campaign ->
            Text(
                text = "${campaign.name}: XP ${campaign.multiplierLabel}" +
                    (campaign.endsAt?.let { IsoInstant.toEpochMillis(it) }?.let { " até ${formatDay(it)}" } ?: ""),
                style = PixTypography.caption,
                color = PixColors.Yellow,
                modifier = Modifier.border(1.dp, PixColors.Yellow).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        progression.previousSeason?.let { previous ->
            Text(
                text = "Na temporada ${previous.season} você chegou ao nível ${previous.level}" +
                    (previous.title?.let { " — $it" } ?: "") + ". A barra recomeça; os pixels ficam.",
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )
        }
    }
}

/**
 * Só o topo do departamento aparece: mostrar o fim da fila envergonha quem
 * está nele e não motiva ninguém. A posição da pessoa vai no título.
 */
@Composable
private fun RankingCard(ranking: Ranking) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Gray700)
            .background(PixColors.Darker),
    ) {
        ranking.top.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (entry.isMe) PixColors.CyanAlpha10 else PixColors.Transparent)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "${index + 1}", style = PixTypography.caption, color = PixColors.Gray400, modifier = Modifier.size(width = 16.dp, height = 16.dp))
                Text(
                    text = if (entry.isMe) "${entry.name} (você)" else entry.name,
                    color = if (entry.isMe) PixColors.Cyan else PixColors.Gray100,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "Nv. ${entry.level}", style = PixTypography.caption, color = PixColors.Gray400)
                Text(text = "${entry.xpTotal} XP", style = PixTypography.caption, color = PixColors.Cyan)
            }
        }
    }
}

@Composable
private fun MissionRow(mission: ProgressionMission) {
    val color = if (mission.done) PixColors.Green else PixColors.Gray700

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, color)
            .background(if (mission.done) PixColors.GreenAlpha20 else PixColors.Darker)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = mission.label, color = PixColors.Gray100)
            Text(
                text = "+${mission.xp} XP",
                style = PixTypography.sectionTitle,
                color = if (mission.done) PixColors.Green else PixColors.Cyan,
            )
        }

        SegmentedBar(fraction = mission.fraction, segments = mission.target.coerceIn(3, 10))

        Text(
            text = "${mission.progress} de ${mission.target} ${mission.unit}".trim(),
            style = PixTypography.caption,
            color = PixColors.Gray400,
        )
    }
}

@Composable
private fun LevelRow(level: ProgressionLevel, current: Boolean) {
    val color = if (level.reached) PixColors.Cyan else PixColors.Gray600

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, color)
            .background(if (level.reached) PixColors.CyanAlpha10 else PixColors.Darker)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp).background(if (level.reached) PixColors.Cyan else PixColors.Gray700),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = level.level.toString(), style = PixTypography.caption, color = if (level.reached) PixColors.Dark else PixColors.Gray300)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = level.title ?: "Nível ${level.level}",
                color = if (level.reached) PixColors.Gray100 else PixColors.Gray300,
            )
            Text(text = "${level.xpRequired} XP", style = PixTypography.caption, color = PixColors.Gray400)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelCoin(size = 12.dp)
            Text(text = level.rewardPixels.toString(), color = PixColors.Yellow)
        }

        if (current) {
            Text(text = "você", style = PixTypography.badgeText, color = PixColors.Cyan)
        }
    }
}

@Composable
private fun EntryRow(entry: XpEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Gray700)
            .background(PixColors.Darker)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = entry.description ?: entry.ruleCode, color = PixColors.Gray100)

            val when_ = entry.createdAt?.let { IsoInstant.toEpochMillis(it) }?.let { formatDay(it) }
            val pixels = entry.grantedPixels?.takeIf { it > 0 }?.let { "+$it pixels" }

            Text(
                text = listOfNotNull(when_, pixels).joinToString(" · "),
                style = PixTypography.caption,
                color = if (pixels != null) PixColors.Yellow else PixColors.Gray400,
            )
        }

        Text(
            text = when {
                entry.isLevelUp -> "nível ${entry.level ?: ""}"
                entry.xp > 0 -> "+${entry.xp} XP"
                else -> "${entry.xp} XP"
            },
            style = PixTypography.sectionTitle,
            color = when {
                entry.isLevelUp -> PixColors.Cyan
                entry.xp >= 0 -> PixColors.Green
                else -> PixColors.Pink
            },
        )
    }
}

/** `dd/mm`, a partir do instante em UTC. */
private fun formatDay(epochMillis: Long): String {
    val (_, month, day) = IsoInstant.civilDateOf(epochMillis)

    return "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}"
}
