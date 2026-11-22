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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.domain.payment.CardValidation
import com.pixstop.mobile.ui.components.*
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CompanyDocumentType
import com.pixstop.mobile.ui.viewmodel.CompanyFormat
import com.pixstop.mobile.ui.viewmodel.RegisterCompanyViewModel
import org.koin.compose.viewmodel.koinViewModel

private val cnpjMask = DigitMask(CompanyFormat::cnpj)
private val cpfMask = DigitMask(CardValidation::formatCpf)
private val phoneMask = DigitMask(CompanyFormat::phone)

/**
 * Cadastro de empresa, igual ao da web: uma tela só, a empresa em cima e
 * quem cadastra embaixo.
 */
@Composable
fun RegisterCompanyScreen(
    viewModel: RegisterCompanyViewModel = koinViewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val enabled = !state.isLoading
    val next = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) onRegisterSuccess()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PixelAuthCard(headerTitle = "SYSTEM.REGISTER", centerContent = true) {
            PixelAuthHeader(
                icon = AppIconType.Business,
                title = "NOVA EMPRESA",
                subtitle = "30 dias grátis para testar!",
                themeColor = PixColors.Pink,
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("SUA EMPRESA")

            PixelInput(
                value = state.companyName,
                onValueChange = viewModel::onCompanyNameChange,
                label = "NOME DA EMPRESA",
                placeholder = "Nome fantasia",
                error = state.fieldErrors["company_name"],
                enabled = enabled,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = next,
                leadingIcon = { FieldIcon(AppIconType.Business) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "TIPO DE DOCUMENTO", style = PixTypography.inputLabel, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompanyDocumentType.entries.forEach { type ->
                    val selected = state.documentType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selected) PixColors.Cyan else PixColors.Dark)
                            .pixelBorder(if (selected) PixColors.Cyan else PixColors.Gray600)
                            .clickable(enabled = enabled) { viewModel.onDocumentTypeChange(type) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = type.label,
                            style = PixTypography.sectionTitle,
                            color = if (selected) PixColors.Dark else PixColors.Gray400,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PixelInput(
                value = state.documentDigits,
                onValueChange = viewModel::onDocumentChange,
                label = state.documentType.label,
                placeholder = if (state.documentType == CompanyDocumentType.Cnpj) "00.000.000/0000-00" else "000.000.000-00",
                error = state.fieldErrors["company_document"],
                enabled = enabled,
                visualTransformation = if (state.documentType == CompanyDocumentType.Cnpj) cnpjMask else cpfMask,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = next,
                leadingIcon = { FieldIcon(AppIconType.Badge) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            PixelInput(
                value = state.phoneDigits,
                onValueChange = viewModel::onPhoneChange,
                label = "WHATSAPP",
                placeholder = "(00) 90000-0000",
                error = state.fieldErrors["company_phone"],
                enabled = enabled,
                visualTransformation = phoneMask,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = next,
                leadingIcon = { FieldIcon(AppIconType.Phone) },
            )

            Spacer(modifier = Modifier.height(28.dp))

            SectionLabel("VOCÊ")

            PixelInput(
                value = state.adminName,
                onValueChange = viewModel::onAdminNameChange,
                label = "SEU NOME",
                placeholder = "Seu nome completo",
                error = state.fieldErrors["admin_name"],
                enabled = enabled,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = next,
                leadingIcon = { FieldIcon(AppIconType.Person) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            PixelInput(
                value = state.adminEmail,
                onValueChange = viewModel::onAdminEmailChange,
                label = "SEU E-MAIL",
                placeholder = "voce@empresa.com",
                error = state.fieldErrors["admin_email"],
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = next,
                leadingIcon = { FieldIcon(AppIconType.Email) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            PixelPasswordInput(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "SENHA",
                error = state.fieldErrors["password"],
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = next,
            )

            if (state.password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                PixelPasswordStrength(password = state.password)
            }

            Spacer(modifier = Modifier.height(20.dp))

            PixelPasswordInput(
                value = state.passwordConfirmation,
                onValueChange = viewModel::onPasswordConfirmationChange,
                label = "CONFIRMAR SENHA",
                error = state.fieldErrors["password_confirmation"],
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.register()
                }),
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.generalError != null && state.fieldErrors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixColors.PinkAlpha20)
                        .pixelBorder(PixColors.Pink)
                        .padding(12.dp),
                ) {
                    Text(text = state.generalError!!, style = PixTypography.errorText)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            PixelButton(
                text = "CRIAR EMPRESA",
                onClick = viewModel::register,
                modifier = Modifier.fillMaxWidth(),
                variant = PixelButtonVariant.Primary,
                enabled = enabled,
                isLoading = state.isLoading,
                loadingText = "CRIANDO...",
            )

            Spacer(modifier = Modifier.height(12.dp))

            LegalNotice()

            PixelDivider()

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Já tem conta? ", style = PixTypography.bodySecondary)
                Text(
                    text = "ENTRAR",
                    style = PixTypography.link.copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.clickable(enabled = enabled) { onNavigateToLogin() },
                )
            }
        }

        if (state.isLoading) {
            PixelBuildLoader()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = PixTypography.footerText, color = PixColors.Gray400, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun FieldIcon(icon: AppIconType) {
    AppIcon(icon = icon, contentDescription = null, tint = PixColors.Gray400, modifier = Modifier.size(16.dp))
}

/** O aceite é gravado no cadastro; a tela precisa dizer isso. */
@Composable
private fun LegalNotice() {
    val uriHandler = LocalUriHandler.current
    val site = ApiConfig.baseUrl.trimEnd('/').removeSuffix("/api")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Ao criar, você aceita os",
            style = PixTypography.bodyMuted,
            textAlign = TextAlign.Center,
        )
        Row {
            Text(
                text = "Termos de Uso",
                style = PixTypography.link.copy(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable { uriHandler.openUri("$site/termos") },
            )
            Text(text = " e a ", style = PixTypography.bodyMuted)
            Text(
                text = "Privacidade",
                style = PixTypography.link.copy(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable { uriHandler.openUri("$site/privacidade") },
            )
        }
    }
}
