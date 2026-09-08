package com.pixstop.mobile.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixstop.mobile.ui.components.*
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.RegisterViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tela de registro de usuário — estilo retro 8-bit terminal.
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = koinViewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Navega quando registrado com sucesso
    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            onRegisterSuccess()
        }
    }

    // QR Code Scanner overlay
    if (uiState.isQrScannerOpen) {
        QrCodeScannerScreen(
            onCodeScanned = { rawValue ->
                viewModel.onQrCodeScanned(rawValue)
            },
            onDismiss = {
                viewModel.closeQrScanner()
            }
        )
        return
    }

    PixelAuthCard(headerTitle = "SYSTEM.REGISTER", centerContent = true) {
        // Auth header
        PixelAuthHeader(
            icon = AppIconType.PersonAdd,
            title = "CRIAR CONTA",
            subtitle = "Registre-se para continuar",
            themeColor = PixColors.Green
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Entrar numa empresa: é por aqui que quase todo mundo chega ──
        CompanyCodeSection(
            companyCode = uiState.companyCode,
            onCompanyCodeChange = viewModel::onCompanyCodeChange,
            isTyping = uiState.isCompanyCodeExpanded,
            onToggleTyping = viewModel::toggleCompanyCodeSection,
            onScanQr = viewModel::openQrScanner,
            error = uiState.fieldErrors["company_code"],
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Nome ──
        PixelInput(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = "NOME",
            placeholder = "Seu nome completo",
            error = uiState.fieldErrors["name"],
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            leadingIcon = {
                AppIcon(
                    icon = AppIconType.Person,
                    contentDescription = null,
                    tint = PixColors.Gray400,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Email ──
        PixelInput(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "E-MAIL",
            placeholder = "seu@email.com",
            error = uiState.fieldErrors["email"],
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
            error = uiState.fieldErrors["password"],
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Confirmar Senha ──
        PixelPasswordInput(
            value = uiState.passwordConfirmation,
            onValueChange = viewModel::onPasswordConfirmationChange,
            label = "CONFIRMAR SENHA",
            error = uiState.fieldErrors["password_confirmation"],
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Erro geral ──
        if (uiState.generalError != null && uiState.fieldErrors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixColors.PinkAlpha20)
                    .pixelBorder(PixColors.Pink)
                    .padding(12.dp)
            ) {
                Text(
                    text = uiState.generalError!!,
                    style = PixTypography.errorText
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Botão Registrar ──
        PixelButton(
            text = "REGISTRAR",
            onClick = viewModel::register,
            modifier = Modifier.fillMaxWidth(),
            variant = PixelButtonVariant.Primary,
            enabled = !uiState.isLoading,
            isLoading = uiState.isLoading,
            loadingText = "REGISTRANDO..."
        )

        // ── Divider ──
        PixelDivider()

        // ── Link para login ──
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Já tem conta? ",
                style = PixTypography.bodySecondary
            )
            Text(
                text = "ENTRAR",
                style = PixTypography.link.copy(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable(enabled = !uiState.isLoading) {
                    onNavigateToLogin()
                }
            )
        }
    }
}

/**
 * Entrar numa empresa, no topo e sem esconderijo.
 *
 * Quase todo mundo que cria conta aqui foi convidado por uma empresa e
 * chegou com um QR na mão. Antes isso vivia atrás de "Tem um código de
 * empresa?", colapsado no fim do formulário: quem tinha o QR não o via, e
 * criava a conta solta.
 */
@Composable
private fun CompanyCodeSection(
    companyCode: String,
    onCompanyCodeChange: (String) -> Unit,
    isTyping: Boolean,
    onToggleTyping: () -> Unit,
    onScanQr: () -> Unit,
    error: String?,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixColors.CyanAlpha10)
            .pixelBorder(PixColors.Cyan)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(
                icon = AppIconType.QrCodeScanner,
                contentDescription = null,
                tint = PixColors.Cyan,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(text = "Entrar numa empresa", style = PixTypography.sectionTitle, color = PixColors.Cyan)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Aponte para o QR Code que a empresa mandou. É ele que liga sua conta à geladeira.",
            style = PixTypography.bodyMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (companyCode.isNotBlank() && !isTyping) {
            // Já tem código: o que interessa agora é conferir e seguir.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = companyCode.uppercase(), style = PixTypography.sectionTitle, color = PixColors.Green)

                Text(
                    text = "TROCAR",
                    style = PixTypography.link.copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.clickable(enabled = enabled) { onToggleTyping() }
                )
            }

            error?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, style = PixTypography.errorText)
            }

            return@Column
        }

        PixelButton(
            text = "ESCANEAR QR CODE",
            onClick = onScanQr,
            modifier = Modifier.fillMaxWidth(),
            variant = PixelButtonVariant.Primary,
            enabled = enabled
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (isTyping) {
            PixelInput(
                value = companyCode,
                onValueChange = onCompanyCodeChange,
                label = "CÓDIGO DA EMPRESA",
                placeholder = "Ex: A1B2C3D4",
                error = error,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "ou digitar o código",
                style = PixTypography.link.copy(textDecoration = TextDecoration.Underline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onToggleTyping() },
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sem empresa? Dá para criar a conta e entrar numa depois.",
            style = PixTypography.footerText
        )
    }
}



