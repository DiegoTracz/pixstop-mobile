package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.LegalDocument
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.LegalConsentViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Aceite dos Termos de Uso e da Política de Privacidade.
 *
 * É bloqueante de propósito: o servidor recusa as rotas de negócio com
 * `consent_required` enquanto faltar aceitar, então deixar a pessoa passar
 * daqui só a levaria a telas vazias. Não há botão de voltar nem de pular —
 * a saída é aceitar ou sair da conta.
 */
@Composable
fun LegalConsentScreen(
    onAccepted: () -> Unit,
    onLogout: () -> Unit,
    viewModel: LegalConsentViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isAccepted) {
        if (state.isAccepted) {
            onAccepted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixColors.Dark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Antes de continuar",
            style = PixTypography.pageTitle,
            color = PixColors.Cyan,
        )

        Text(
            text = "Para usar o app é preciso aceitar os documentos abaixo.",
            style = PixTypography.bodySecondary,
        )

        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = PixColors.Cyan)
            }

            state.documents.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = state.error ?: "Não foi possível carregar os documentos.",
                    style = PixTypography.errorText,
                )

                PixelButton(
                    text = "Tentar de novo",
                    onClick = viewModel::load,
                    variant = PixelButtonVariant.Secondary,
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.documents.forEach { document ->
                        DocumentBlock(document)
                    }
                }

                AcceptCheckbox(
                    checked = state.accepted,
                    onToggle = { viewModel.toggleAccepted(!state.accepted) },
                )

                state.error?.let { message ->
                    Text(text = message, style = PixTypography.errorText)
                }

                PixelButton(
                    text = "Aceitar e continuar",
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    isLoading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )

                PixelButton(
                    text = "Sair da conta",
                    onClick = onLogout,
                    variant = PixelButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Um documento: título, versão e o texto inteiro dentro de uma área rolável
 * própria, para que a lista não vire uma página só.
 */
@Composable
private fun DocumentBlock(document: LegalDocument) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (document.accepted) PixColors.Gray700 else PixColors.Gray600)
            .background(PixColors.Darker)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = document.title, style = PixTypography.sectionTitle, color = PixColors.Cyan)

            Text(
                text = if (document.accepted) "aceito" else "v${document.version}",
                style = PixTypography.caption,
                color = if (document.accepted) PixColors.Green else PixColors.Gray400,
            )
        }

        Text(
            text = document.content,
            style = PixTypography.bodyMuted,
            modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun AcceptCheckbox(checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .size(24.dp)
                .border(2.dp, if (checked) PixColors.Cyan else PixColors.Gray500)
                .background(if (checked) PixColors.Cyan else PixColors.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (checked) {
                AppIcon(
                    icon = AppIconType.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PixColors.Dark,
                )
            }
        }

        Text(
            text = "Li e aceito os documentos acima.",
            style = PixTypography.bodyRegular,
        )
    }
}
