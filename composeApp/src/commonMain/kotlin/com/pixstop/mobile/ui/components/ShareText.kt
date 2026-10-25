package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * Manda um texto para outro app (o WhatsApp do grupo que compra, por exemplo).
 *
 * Devolve uma função; chamar abre o compartilhamento do sistema. Onde não há
 * compartilhamento, o texto vai para a área de transferência, e a função
 * devolve `false` para a tela dizer "copiado".
 */
@Composable
expect fun rememberShareText(): (String) -> Boolean
