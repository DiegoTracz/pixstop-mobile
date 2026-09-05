package com.pixstop.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.pixstop.mobile.domain.payment.CardBrand
import com.pixstop.mobile.domain.payment.CardInput
import com.pixstop.mobile.domain.payment.CardValidation
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Os dados de um cartão novo.
 *
 * O formulário é nosso, e não uma tela do gateway, porque o app inteiro tem
 * uma identidade e trocá-la no momento do pagamento assusta. O que o gateway
 * faz é receber o número direto do aparelho e devolver um token — nada aqui
 * chega ao nosso servidor.
 */
@Composable
fun CardForm(
    card: CardInput,
    errors: Map<String, String>,
    onChange: (CardInput) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PixelInput(
            value = card.number,
            onValueChange = { onChange(card.copy(number = it.digits(19))) },
            label = "NÚMERO DO CARTÃO",
            placeholder = "0000 0000 0000 0000",
            error = errors[CardInput.FIELD_NUMBER],
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = DigitMask(CardValidation::formatCardNumber),
            modifier = Modifier.fillMaxWidth(),
        )

        if (card.brand != CardBrand.Unknown) {
            Text(text = card.brand.label, style = PixTypography.caption, color = PixColors.Gray400)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PixelInput(
                value = card.expiry,
                onValueChange = { onChange(card.copy(expiry = it.digits(4))) },
                label = "VALIDADE",
                placeholder = "12/30",
                error = errors[CardInput.FIELD_EXPIRY],
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = DigitMask(CardValidation::formatExpiry),
                modifier = Modifier.weight(1f),
            )

            PixelInput(
                value = card.securityCode,
                onValueChange = { onChange(card.copy(securityCode = it.digits(4))) },
                label = "CÓD. SEGURANÇA",
                placeholder = "123",
                error = errors[CardInput.FIELD_CVV],
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1f),
            )
        }

        PixelInput(
            value = card.holderName,
            onValueChange = { onChange(card.copy(holderName = it.uppercase())) },
            label = "NOME NO CARTÃO",
            placeholder = "COMO ESTÁ IMPRESSO",
            error = errors[CardInput.FIELD_HOLDER],
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )

        PixelInput(
            value = card.documentNumber,
            onValueChange = { onChange(card.copy(documentNumber = it.digits(11))) },
            label = "CPF DO TITULAR",
            placeholder = "000.000.000-00",
            error = errors[CardInput.FIELD_DOCUMENT],
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = DigitMask(CardValidation::formatCpf),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Os dados do cartão vão direto ao processador de pagamento. " +
                "O Pixstop não guarda o número.",
            style = PixTypography.caption,
            color = PixColors.Gray400,
        )
    }
}

/** Só os dígitos, até onde o campo aceita. */
private fun String.digits(max: Int): String = filter { it.isDigit() }.take(max)
