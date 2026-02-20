package com.pixstop.mobile.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.theme.AppBranding
import com.pixstop.mobile.ui.theme.AppColors

/**
 * Tela de Splash com design minimalista moderno.
 *
 * Exibe o logo e nome do app em fundo branco limpo.
 *
 * Para personalizar o nome e ícone, edite: ui/theme/AppBranding.kt
 *
 * @param onSplashFinished Callback chamado quando a splash termina (após delay ou carregamento).
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animação de fade-in
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000
        ),
        label = "splash_alpha"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        // Tempo mínimo de exibição da splash
        delay(1500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .padding(horizontal = 48.dp)
        ) {
            // Logo do aplicativo (configurado em AppBranding.kt)
            Surface(
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = AppColors.Primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AppIcon(
                        icon = AppBranding.APP_ICON,
                        contentDescription = "Logo",
                        modifier = Modifier.size(60.dp),
                        tint = AppColors.OnPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nome do aplicativo (configurado em AppBranding.kt)
            Text(
                text = AppBranding.APP_NAME,
                color = AppColors.Primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Indicador de carregamento sutil
            CircularProgressIndicator(
                color = AppColors.Primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
