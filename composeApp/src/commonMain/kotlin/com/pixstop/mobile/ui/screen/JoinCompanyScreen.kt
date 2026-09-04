package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.core.config.CompanyCodeParser
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.QrCodeScannerScreen
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.SessionViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Entrar numa empresa pelo código ou pelo QR impresso.
 *
 * É a única saída para quem está logado sem empresa nenhuma, e também o
 * caminho de quem trabalha em duas.
 */
@Composable
fun JoinCompanyScreen(
    onJoined: () -> Unit,
    onBack: (() -> Unit)? = null,
    sessionViewModel: SessionViewModel = koinViewModel(),
) {
    val state by sessionViewModel.uiState.collectAsState()

    var code by remember { mutableStateOf("") }
    var scannerOpen by remember { mutableStateOf(false) }
    var joinedName by remember { mutableStateOf<String?>(null) }

    // A troca só termina quando o `/me` volta com a empresa nova; só então a
    // navegação avança, senão a próxima tela abriria sem empresa ativa.
    LaunchedEffect(joinedName, state.company?.id) {
        if (joinedName != null && state.company != null) {
            onJoined()
        }
    }

    if (scannerOpen) {
        QrCodeScannerScreen(
            onCodeScanned = { raw ->
                code = CompanyCodeParser.parse(raw)
                scannerOpen = false
                sessionViewModel.joinCompany(code) { joinedName = it }
            },
            onDismiss = { scannerOpen = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixColors.Dark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Entrar na empresa",
            style = PixTypography.pageTitle,
            modifier = Modifier.padding(top = 32.dp, bottom = 8.dp),
        )

        Text(
            text = "Peça o código ao administrador da sua empresa, ou aponte a câmera para o QR impresso.",
            style = PixTypography.caption,
            color = PixColors.Gray300,
            textAlign = TextAlign.Center,
        )

        PixelInput(
            value = code,
            onValueChange = { code = it.uppercase().filter(Char::isLetterOrDigit) },
            label = "Código da empresa",
            placeholder = "ABC12345",
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { message ->
            Text(
                text = message,
                style = PixTypography.errorText,
                color = PixColors.Pink,
                textAlign = TextAlign.Center,
            )
        }

        PixelButton(
            text = "Entrar",
            onClick = { sessionViewModel.joinCompany(code) { joinedName = it } },
            enabled = code.length >= 4 && !state.isSwitching,
            isLoading = state.isSwitching,
            loadingText = "Entrando...",
            modifier = Modifier.fillMaxWidth(),
        )

        PixelButton(
            text = "Ler QR code",
            onClick = {
                sessionViewModel.dismissError()
                scannerOpen = true
            },
            variant = PixelButtonVariant.Secondary,
            enabled = !state.isSwitching,
            modifier = Modifier.fillMaxWidth(),
        )

        if (onBack != null) {
            PixelButton(
                text = "Voltar",
                onClick = onBack,
                variant = PixelButtonVariant.Secondary,
                enabled = !state.isSwitching,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
