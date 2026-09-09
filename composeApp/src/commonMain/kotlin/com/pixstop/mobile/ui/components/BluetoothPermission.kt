package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * Pede as permissões de Bluetooth e devolve a função que faz o pedido.
 *
 * Só é chamada quando a pessoa toca em "Abrir por Bluetooth": pedir rádio na
 * abertura do app assusta e o sistema penaliza. Onde a plataforma não exige
 * nada, a função responde que sim na hora.
 */
@Composable
expect fun rememberBluetoothPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit
