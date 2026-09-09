package com.pixstop.mobile.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

/**
 * Do Android 12 em diante, escanear e conectar são permissões próprias; antes
 * disso, escanear exigia localização, porque um rádio por perto diz onde a
 * pessoa está. Já concedidas, o pedido responde sem abrir diálogo nenhum.
 */
@Composable
actual fun rememberBluetoothPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        callback.value(granted.values.all { it })
    }

    return {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            callback.value(true)
        } else {
            launcher.launch(missing.toTypedArray())
        }
    }
}
