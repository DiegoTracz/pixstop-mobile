package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.AccountUser
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelDivider
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelPasswordInput
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ProfileUiState
import com.pixstop.mobile.ui.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Dados da conta e troca de senha.
 *
 * As duas metades salvam separado: são chamadas diferentes no servidor e
 * falhar numa não pode desfazer a outra.
 */
@Composable
fun ProfileScreen(
    user: AccountUser?,
    onProfileSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(user?.id) {
        user?.let { viewModel.start(it.name, it.email) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Seus dados", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        PixelInput(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = "Nome",
            enabled = !state.isSavingProfile,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )

        PixelInput(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "E-mail",
            enabled = !state.isSavingProfile,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )

        Feedback(message = state.profileMessage, error = state.profileError)

        PixelButton(
            text = "Salvar",
            onClick = { viewModel.saveProfile(onProfileSaved) },
            enabled = state.canSaveProfile,
            isLoading = state.isSavingProfile,
            modifier = Modifier.fillMaxWidth(),
        )

        PixelDivider(text = "")

        Text(text = "Trocar a senha", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        PixelPasswordInput(
            value = state.currentPassword,
            onValueChange = viewModel::onCurrentPasswordChange,
            label = "Senha atual",
            enabled = !state.isSavingPassword,
            modifier = Modifier.fillMaxWidth(),
        )

        PixelPasswordInput(
            value = state.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            label = "Nova senha",
            enabled = !state.isSavingPassword,
            error = "Use ao menos ${ProfileUiState.MIN_PASSWORD} caracteres."
                .takeIf { state.newPassword.isNotEmpty() && state.newPassword.length < ProfileUiState.MIN_PASSWORD },
            modifier = Modifier.fillMaxWidth(),
        )

        PixelPasswordInput(
            value = state.confirmation,
            onValueChange = viewModel::onConfirmationChange,
            label = "Confirme a nova senha",
            enabled = !state.isSavingPassword,
            error = "As senhas não são iguais.".takeIf { state.confirmation.isNotEmpty() && !state.passwordsMatch },
            modifier = Modifier.fillMaxWidth(),
        )

        Feedback(message = state.passwordMessage, error = state.passwordError)

        PixelButton(
            text = "Alterar senha",
            onClick = viewModel::savePassword,
            enabled = state.canSavePassword,
            isLoading = state.isSavingPassword,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Feedback(message: String?, error: String?) {
    error?.let {
        Text(text = it, style = PixTypography.errorText)
        return
    }

    message?.let {
        Text(text = it, style = PixTypography.caption, color = PixColors.Green)
    }
}
