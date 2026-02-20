package com.pixstop.mobile.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pixstop.mobile.App
import com.pixstop.mobile.core.config.ApiConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Configure a URL da sua API Laravel aqui:
            // ApiConfig.configure("https://seu-servidor.com/api")

            // Para emulador Android acessando localhost:
            // ApiConfig.configure("http://10.0.2.2/api")

            App()
        }
    }
}
