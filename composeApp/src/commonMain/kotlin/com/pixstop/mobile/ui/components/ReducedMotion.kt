package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * A pessoa pediu ao sistema menos movimento: no Android, a escala de animação
 * zerada nas opções; no iPhone, "Reduzir movimento". Quem anima decorando (o
 * mascote) para quieto, e o texto ao lado é quem diz o estado.
 */
@Composable
expect fun rememberReducedMotion(): Boolean
