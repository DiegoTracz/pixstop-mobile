package com.pixstop.mobile.ui.theme

import androidx.compose.runtime.Composable

/**
 * Os ícones da barra de status seguem o tema do aplicativo, não o do aparelho.
 *
 * O app desenha atrás das barras do sistema, então quem escolhe "claro" com o
 * telefone no escuro ficaria com relógio e bateria brancos em cima de um fundo
 * branco — sumiriam.
 */
@Composable
expect fun SystemBarsEffect(dark: Boolean)
