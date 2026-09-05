package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.decodeBase64Image
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.OrderViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Desfecho do pedido.
 *
 * Pago com saldo ou pixels, já chega resolvido. Pago em PIX, mostra o código e
 * fica perguntando ao servidor até o pagamento cair.
 */
@Composable
fun OrderScreen(
    orderId: Long,
    onBack: () -> Unit,
    viewModel: OrderViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(orderId) {
        viewModel.load(orderId)
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Pedido", onBack = onBack)

        when {
            state.isLoading && state.order == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PixColors.Cyan)
            }

            state.order == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error ?: "Pedido não encontrado.", style = PixTypography.errorText)
            }

            else -> {
                val order = state.order!!

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    StatusHeader(order)

                    // Só enquanto o pagamento é esperado: um pedido já pago
                    // não pode ficar dizendo que aguarda.
                    if (order.isPending) {
                        order.pix?.let { pix ->
                            PixBlock(
                                code = pix.code,
                                qrCodeBase64 = pix.qrCodeBase64,
                                secondsLeft = state.secondsLeft,
                                onCopy = { clipboard.setText(AnnotatedString(pix.code)) },
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Itens", style = PixTypography.sectionTitle, color = PixColors.Cyan)

                        order.items.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${line.quantity}× ${line.productName}",
                                    style = PixTypography.bodySecondary,
                                    modifier = Modifier.weight(1f),
                                )

                                Text(text = formatMoney(line.subtotal), color = PixColors.Gray100)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Pagamento", style = PixTypography.sectionTitle, color = PixColors.Cyan)

                        order.paymentMethodLabel?.let {
                            Line("Forma", it)
                        }

                        if (order.pixels > 0) {
                            Line("Pixels usados", order.pixels.toString(), PixColors.Yellow)
                        }

                        if (order.balance > 0) {
                            Line("Saldo", formatMoney(order.balance), PixColors.Green)
                        }

                        if (order.money > 0) {
                            Line("Dinheiro", formatMoney(order.money))
                        }

                        // A taxa foi mostrada no fechamento e faz parte do que
                        // a pessoa aceitou pagar; some dela aqui seria esconder
                        // a diferença entre o preço do produto e o cobrado.
                        if (order.cardFee > 0) {
                            Line("Taxa do cartão", formatMoney(order.cardFee), PixColors.Gray300)
                        }

                        Line("Produtos", formatMoney(order.productsTotal))
                    }

                    order.cancellationReason?.let {
                        Text(text = it, style = PixTypography.errorText)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixColors.Darker)
                        .navigationBarsPadding()
                        .padding(20.dp),
                ) {
                    PixelButton(
                        text = "Voltar",
                        onClick = onBack,
                        variant = PixelButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusHeader(order: Order) {
    val color = when (order.status) {
        OrderStatus.Paid, OrderStatus.Delivered -> PixColors.Green
        OrderStatus.Canceled -> PixColors.Pink
        else -> PixColors.Yellow
    }

    val icon = when (order.status) {
        OrderStatus.Paid, OrderStatus.Delivered -> AppIconType.Check
        OrderStatus.Canceled -> AppIconType.Close
        else -> AppIconType.Info
    }

    Column(
        modifier = Modifier.fillMaxWidth().border(2.dp, color).background(PixColors.Darker).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(color),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                icon = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = PixColors.Dark,
            )
        }

        Text(text = order.statusLabel, style = PixTypography.sectionTitle, color = color)

        order.transactionId?.let {
            Text(text = it, style = PixTypography.caption, color = PixColors.Gray400)
        }

        if (order.isPaid) {
            Text(
                text = "Pode retirar o produto.",
                style = PixTypography.bodySecondary,
            )
        }
    }
}

/**
 * O "copia e cola" do PIX.
 *
 * O código em texto vem antes da imagem porque é o que funciona em qualquer
 * banco, inclusive quando a pessoa está pagando pelo próprio celular e não
 * tem como apontar a câmera para a própria tela.
 */
@Composable
private fun PixBlock(
    code: String,
    qrCodeBase64: String?,
    secondsLeft: Long?,
    onCopy: () -> Unit,
) {
    // Decodifica uma vez, não a cada segundo do contador.
    val qrCode = remember(qrCodeBase64) { decodeBase64Image(qrCodeBase64) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Cyan)
            .background(PixColors.Darker)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Pague com PIX",
            style = PixTypography.sectionTitle,
            color = PixColors.Cyan,
            modifier = Modifier.fillMaxWidth(),
        )

        if (secondsLeft != null) {
            Text(
                text = if (secondsLeft > 0) "Expira em ${formatCountdown(secondsLeft)}" else "Este PIX expirou.",
                style = PixTypography.caption,
                color = if (secondsLeft > 0) PixColors.Yellow else PixColors.Pink,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, if (secondsLeft > 0) PixColors.Yellow else PixColors.Pink)
                    .padding(8.dp),
                textAlign = TextAlign.Center,
            )
        }

        if (qrCode != null) {
            // O fundo branco não é enfeite: o leitor do banco precisa do
            // contraste, e o app inteiro é escuro.
            Image(
                bitmap = qrCode,
                contentDescription = "QR code do PIX",
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .background(PixColors.White)
                    .padding(12.dp)
                    .size(200.dp),
            )
        }

        Text(
            text = if (qrCode != null) {
                "Aponte a câmera do seu banco para o código, ou copie e cole. " +
                    "A confirmação aparece aqui automaticamente."
            } else {
                "Copie o código e cole no aplicativo do seu banco. " +
                    "A confirmação aparece aqui automaticamente."
            },
            style = PixTypography.caption,
            color = PixColors.Gray400,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = code,
            style = PixTypography.caption,
            color = PixColors.Gray300,
            modifier = Modifier
                .fillMaxWidth()
                .background(PixColors.Dark)
                .padding(12.dp),
            maxLines = 4,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )

        PixelButton(
            text = "Copiar chave PIX",
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PixColors.Yellow, strokeWidth = 2.dp)

            Text(text = "Aguardando o pagamento…", style = PixTypography.caption, color = PixColors.Yellow)
        }
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = PixColors.Gray100,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = PixTypography.bodySecondary, color = PixColors.Gray300)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (color == PixColors.Yellow) {
                PixelCoin(size = 12.dp)
            }

            Text(text = value, color = color)
        }
    }
}
