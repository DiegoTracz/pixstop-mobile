package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Input retro 8-bit com label pixel, borda sólida, glow no foco, erro em pink.
 * Cantos retos (0dp radius) — estética pixel art.
 */
@Composable
fun PixelInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        error != null -> PixColors.Pink
        isFocused -> PixColors.Cyan
        else -> PixColors.Gray600
    }

    Column(modifier = modifier) {
        // Label
        Text(
            text = label.uppercase(),
            style = PixTypography.inputLabel,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Input container with glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFocused || error != null) {
                        Modifier.neonGlow(
                            color = if (error != null) PixColors.Pink else PixColors.Cyan,
                            radius = 8.dp,
                            alpha = 0.15f
                        )
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixColors.Dark)
                    .border(2.dp, borderColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = PixTypography.placeholder
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = PixTypography.inputValue,
                        singleLine = singleLine,
                        enabled = enabled,
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        cursorBrush = SolidColor(PixColors.Cyan)
                    )
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }

        // Error message
        if (error != null) {
            Text(
                text = error,
                style = PixTypography.errorText,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}



