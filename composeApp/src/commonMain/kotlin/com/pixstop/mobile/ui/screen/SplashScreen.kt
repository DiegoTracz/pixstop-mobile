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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.theme.AppBranding
import com.pixstop.mobile.ui.theme.AppColors

/**
 * Tela de Splash exibida ao iniciar o aplicativo.
 *
 * Mostra o logo/nome do app com a cor primária enquanto carrega os dados iniciais.
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
            .background(AppColors.Primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            // Logo do aplicativo (configurado em AppBranding.kt)
            Surface(
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = AppColors.OnPrimary.copy(alpha = 0.2f)
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
                color = AppColors.OnPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Indicador de carregamento
            CircularProgressIndicator(
                color = AppColors.OnPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
