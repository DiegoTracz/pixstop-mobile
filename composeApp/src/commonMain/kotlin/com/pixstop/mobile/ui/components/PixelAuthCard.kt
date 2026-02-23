package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Card terminal retro para telas de autenticação — fullscreen no mobile.
 *
 * Ocupa TODA a tela: header fixo no topo, conteúdo scrollável.
 * Inclui header bar com 3 pontos coloridos (pink, yellow, green) e título.
 */
@Composable
fun PixelAuthCard(
    modifier: Modifier = Modifier,
    headerTitle: String = "SYSTEM.AUTH",
    centerContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PixColors.Gray900)
            .statusBarsPadding()
    ) {
        // Terminal header bar (fixo no topo)
        TerminalHeaderBar(title = headerTitle)

        // Content area scrollável
        if (centerContent) {
            // Centralizado — para telas com pouco conteúdo (login)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .defaultMinSize(minHeight = maxHeight)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    content = content
                )
            }
        } else {
            // Top — para telas com muito conteúdo (registro)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

/**
 * Terminal header bar com pontos coloridos e título.
 */
@Composable
private fun TerminalHeaderBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixColors.Gray800)
            .drawBehind {
                // 1dp bottom border cyan 20%
                drawRect(
                    PixColors.Cyan.copy(alpha = 0.2f),
                    Offset(0f, size.height - 1.dp.toPx()),
                    Size(size.width, 1.dp.toPx())
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 3 colored dots
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(PixColors.Pink)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(PixColors.Yellow)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(PixColors.Green)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = PixTypography.terminalHeader
        )
    }
}


