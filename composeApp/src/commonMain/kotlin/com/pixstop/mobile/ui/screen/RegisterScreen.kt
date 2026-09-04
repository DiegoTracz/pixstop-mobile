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

        Spacer(modifier = Modifier.height(32.dp))

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

        // ── Seção colapsável: Código de empresa ──
        CompanyCodeSection(
            companyCode = uiState.companyCode,
            onCompanyCodeChange = viewModel::onCompanyCodeChange,
            isExpanded = uiState.isCompanyCodeExpanded,
            onToggle = viewModel::toggleCompanyCodeSection,
            onScanQr = viewModel::openQrScanner,
            error = uiState.fieldErrors["company_code"],
            enabled = !uiState.isLoading
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
 * Seção colapsável do código de empresa com botão de QR scanner.
 */
@Composable
private fun CompanyCodeSection(
    companyCode: String,
    onCompanyCodeChange: (String) -> Unit,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onScanQr: () -> Unit,
    error: String?,
    enabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Toggle header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tem um código de empresa?",
                style = PixTypography.bodyMuted
            )
            AppIcon(
                icon = if (isExpanded) AppIconType.ChevronUp else AppIconType.ChevronDown,
                contentDescription = null,
                tint = PixColors.Cyan,
                modifier = Modifier.size(16.dp)
            )
        }

        // Expandable content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Input do código
                    Box(modifier = Modifier.weight(1f)) {
                        PixelInput(
                            value = companyCode,
                            onValueChange = onCompanyCodeChange,
                            label = "CÓDIGO DA EMPRESA",
                            placeholder = "Ex: A1B2C3D4",
                            error = error,
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botão QR Scanner
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Spacer para alinhar com o input (pula a label)
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PixColors.Gray700)
                                .pixelBorder(PixColors.Cyan)
                                .clickable(enabled = enabled) { onScanQr() },
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                icon = AppIconType.QrCodeScanner,
                                contentDescription = "Escanear QR Code",
                                tint = PixColors.Cyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Escaneie o QR Code ou digite o código manualmente",
                    style = PixTypography.footerText
                )
            }
        }
    }
}



