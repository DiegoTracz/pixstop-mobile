package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixstop.mobile.ui.components.*
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tela de Login — estilo retro 8-bit terminal.
 * O PixelAuthCard ocupa toda a tela.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    PixelAuthCard(centerContent = true) {
        Spacer(modifier = Modifier.height(16.dp))

        // Branding
        Text(
            text = "PixStop",
            style = PixTypography.pageTitle
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Auth header
        PixelAuthHeader(
            icon = AppIconType.Lock,
            title = "LOGIN",
            subtitle = "Acesse sua conta",
            themeColor = PixColors.Cyan
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Email ──
        PixelInput(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "E-MAIL",
            placeholder = "seu@email.com",
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            leadingIcon = {
                AppIcon(
                    icon = AppIconType.Email,
                    contentDescription = null,
                    tint = PixColors.Gray400,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Senha ──
        PixelPasswordInput(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "SENHA",
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.login()
                }
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Erro ──
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixColors.PinkAlpha20)
                    .pixelBorder(PixColors.Pink)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = uiState.error!!,
                        style = PixTypography.errorText
                    )

                    if (uiState.isOfflineMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CONTINUAR OFFLINE",
                            style = PixTypography.link.copy(
                                textDecoration = TextDecoration.Underline,
                                color = PixColors.Yellow
                            ),
                            modifier = Modifier.clickable { viewModel.continueOffline() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Botão Login ──
        PixelButton(
            text = "ENTRAR",
            onClick = viewModel::login,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            isLoading = uiState.isLoading,
            loadingText = "ENTRANDO..."
        )

        // ── Divider ──
        PixelDivider()

        // ── Link para registro ──
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Não tem conta? ",
                style = PixTypography.bodySecondary
            )
            Text(
                text = "CRIAR CONTA",
                style = PixTypography.link.copy(
                    textDecoration = TextDecoration.Underline,
                    color = PixColors.Green
                ),
                modifier = Modifier.clickable(enabled = !uiState.isLoading) {
                    onNavigateToRegister()
                }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Footer
        Text(
            text = "© 2026 PixStop",
            style = PixTypography.footerText
        )
    }
}
