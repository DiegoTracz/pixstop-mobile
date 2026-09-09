package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * O iOS ainda não abre a geladeira por Bluetooth (B3): o conector diz que não
 * dá, a tela não oferece o caminho, e nada aqui é chamado.
 */
@Composable
actual fun rememberBluetoothPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit = { onResult(false) }
