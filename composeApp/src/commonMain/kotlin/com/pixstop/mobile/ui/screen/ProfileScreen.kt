package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pixstop.mobile.core.storage.ThemeStore
import com.pixstop.mobile.domain.model.AccountUser
import androidx.compose.ui.window.Dialog
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelDivider
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelPasswordInput
import com.pixstop.mobile.ui.components.PixelCameraScreen
import com.pixstop.mobile.ui.components.decodeBase64Image
import com.pixstop.mobile.ui.components.rememberPhotoPicker
import com.pixstop.mobile.ui.theme.AppThemeMode
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ProfileUiState
import com.pixstop.mobile.ui.viewmodel.PixelAvatarUiState
import com.pixstop.mobile.ui.viewmodel.PixelAvatarViewModel
import com.pixstop.mobile.ui.viewmodel.ProfileViewModel
import org.koin.compose.koinInject
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
    onAccountDeleted: () -> Unit = {},
    onOpenCards: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    avatarViewModel: PixelAvatarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val avatar by avatarViewModel.uiState.collectAsState()
    val pickPhoto = rememberPhotoPicker { avatarViewModel.uploadPhoto(it, onProfileSaved) }
    val themeStore: ThemeStore = koinInject()
    val themeMode by themeStore.mode.collectAsState()

    LaunchedEffect(user?.id) {
        user?.let { viewModel.start(it.name, it.email) }
    }

    // Conta excluída: não há mais nada para esta tela mostrar.
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onAccountDeleted()
        }
    }

    // A câmera toma a tela inteira: mirar o próprio rosto num quadradinho
    // dentro de um formulário não funciona.
    if (avatar.isCameraOpen) {
        PixelCameraScreen(
            onPhoto = avatarViewModel::onPhoto,
            onDismiss = avatarViewModel::closeCamera,
        )

        return
    }

    if (state.isDeleteOpen) {
        DeleteAccountDialog(
            password = state.deletePassword,
            isDeleting = state.isDeleting,
            canDelete = state.canDelete,
            error = state.deleteError,
            onPasswordChange = viewModel::onDeletePasswordChange,
            onConfirm = viewModel::deleteAccount,
            onDismiss = viewModel::dismissDelete,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AvatarSection(
            state = avatar,
            userName = user?.name,
            avatarUrl = user?.avatarUrl,
            onOpenCamera = avatarViewModel::openCamera,
            onPickPhoto = pickPhoto,
            onApply = { avatarViewModel.apply(onProfileSaved) },
            onDiscard = avatarViewModel::discardPreview,
        )

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

        AppearanceSection(selected = themeMode, onSelect = themeStore::set)

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

        PixelDivider(text = "")

        Text(text = "Meus cartões", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        Text(
            text = "Os cartões guardados no fechamento: escolha o padrão ou remova o que não quer mais ver.",
            style = PixTypography.bodySecondary,
        )

        PixelButton(
            text = "Gerenciar cartões",
            onClick = onOpenCards,
            variant = PixelButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )

        PixelDivider(text = "")

        Text(text = "Excluir a conta", style = PixTypography.sectionTitle, color = PixColors.Pink)

        Text(
            text = "Sua conta sai do ar na hora. Os dados são apagados em definitivo " +
                "depois de ${state.purgeAfterDays} dias, e até lá dá para voltar atrás.",
            style = PixTypography.bodySecondary,
        )

        PixelButton(
            text = "Excluir minha conta",
            onClick = viewModel::openDelete,
            variant = PixelButtonVariant.Destructive,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Confirmação da exclusão.
 *
 * Pede a senha porque é o que separa o pedido do titular de alguém que pegou
 * o celular destravado.
 */
/**
 * O avatar em pixel art.
 *
 * A foto vira um retrato 8-bit e fica esperando: quem tirou vê o resultado
 * antes de ele virar a foto do perfil, e pode tirar outra sem ter perdido o
 * avatar que já tinha.
 */
/**
 * Claro, escuro ou o que o aparelho estiver usando.
 *
 * O padrão é acompanhar o sistema: quem liga o modo escuro à noite espera que
 * o aplicativo acompanhe sem ter de vir aqui.
 */
@Composable
private fun AppearanceSection(selected: AppThemeMode, onSelect: (AppThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Aparência", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { mode ->
                val isSelected = mode == selected

                Text(
                    text = mode.label.uppercase(),
                    style = PixTypography.badgeText,
                    color = if (isSelected) PixColors.Dark else PixColors.Cyan,
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, PixColors.Cyan)
                        .background(if (isSelected) PixColors.Cyan else PixColors.Transparent)
                        .clickable { onSelect(mode) }
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AvatarSection(
    state: PixelAvatarUiState,
    userName: String?,
    avatarUrl: String?,
    onOpenCamera: () -> Unit,
    onPickPhoto: () -> Unit,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Seu avatar", style = PixTypography.sectionTitle, color = PixColors.Cyan)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarThumb(preview = state.preview, avatarUrl = avatarUrl, userName = userName)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (state.hasPreview) "Ficou assim" else "Vire um personagem 8-bit",
                    style = PixTypography.bodyRegular,
                    color = PixColors.Gray100,
                )

                Text(
                    text = if (state.hasPreview) {
                        "Use este ou tire outra foto."
                    } else {
                        "Tire uma foto e a transformamos em pixel art."
                    },
                    style = PixTypography.bodyMuted,
                )
            }
        }

        Feedback(message = state.message, error = state.error)

        if (state.hasPreview) {
            PixelButton(
                text = "Usar este avatar",
                onClick = onApply,
                enabled = !state.isWorking,
                isLoading = state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            )

            PixelButton(
                text = "Tirar outra",
                onClick = { onDiscard(); onOpenCamera() },
                variant = PixelButtonVariant.Secondary,
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PixelButton(
                text = "Abrir a câmera pixel art",
                onClick = onOpenCamera,
                enabled = !state.isWorking,
                isLoading = state.isWorking,
                loadingText = "Desenhando...",
                modifier = Modifier.fillMaxWidth(),
            )

            PixelButton(
                text = "Escolher uma foto",
                onClick = onPickPhoto,
                variant = PixelButtonVariant.Secondary,
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * O que a pessoa está vendo agora: a prévia recém-gerada, se houver; senão a
 * foto que já está no perfil; senão a inicial no quadrado.
 */
@Composable
private fun AvatarThumb(preview: String?, avatarUrl: String?, userName: String?) {
    val bitmap = remember(preview) { decodeBase64Image(preview) }

    Box(
        modifier = Modifier.size(96.dp).border(2.dp, PixColors.Cyan).background(PixColors.Gray800),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = "Prévia do avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            !avatarUrl.isNullOrBlank() -> AsyncImage(
                model = avatarUrl,
                contentDescription = "Sua foto",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Gray500 é o cinza mais apagado da escala: em cima do quadrado
            // branco do modo claro a inicial simplesmente não aparecia.
            else -> Text(
                text = userName?.take(1)?.uppercase().orEmpty(),
                style = PixTypography.pageTitle,
                color = PixColors.Gray300,
            )
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    password: String,
    isDeleting: Boolean,
    canDelete: Boolean,
    error: String?,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, PixColors.Pink)
                .background(PixColors.Darker)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Excluir a conta", style = PixTypography.sectionTitle, color = PixColors.Pink)

            Text(
                text = "Você perde o acesso às empresas, ao saldo e aos pixels. " +
                    "Confirme com a sua senha.",
                style = PixTypography.bodySecondary,
            )

            PixelPasswordInput(
                value = password,
                onValueChange = onPasswordChange,
                label = "Senha",
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Text(text = it, style = PixTypography.errorText)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(
                    text = "Voltar",
                    onClick = onDismiss,
                    variant = PixelButtonVariant.Secondary,
                    enabled = !isDeleting,
                    modifier = Modifier.weight(1f),
                )

                PixelButton(
                    text = "Excluir",
                    onClick = onConfirm,
                    variant = PixelButtonVariant.Destructive,
                    enabled = canDelete,
                    isLoading = isDeleting,
                    modifier = Modifier.weight(1f),
                )
            }
        }
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
