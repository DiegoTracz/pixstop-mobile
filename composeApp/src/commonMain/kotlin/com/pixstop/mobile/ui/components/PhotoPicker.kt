package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable

/**
 * Escolher uma foto que já está no aparelho.
 *
 * Nem toda foto de perfil precisa ser tirada na hora: muita gente já tem a
 * foto de que gosta. Devolve os bytes do arquivo escolhido, ou nada quando a
 * pessoa desiste.
 */
@Composable
expect fun rememberPhotoPicker(onPhoto: (ByteArray) -> Unit): () -> Unit
