package com.pixstop.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.FridgeLight
import com.pixstop.mobile.domain.model.LedSettings
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * A fita LED, numa gaveta que sobe (Fase 9.10).
 *
 * O controle é uma coisa só e ocupa a tela inteira: numa gaveta ele aparece
 * quando se quer mexer na fita e some quando acaba, sem tirar a pessoa de
 * onde ela estava — que é a tela da geladeira, com o resto dos mostradores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedSheet(
    led: LedSettings,
    /** A cor acesa que ainda não foi salva. */
    picked: String?,
    pressing: String?,
    lastSent: String?,
    online: Boolean,
    onChooseProfile: (Long) -> Unit,
    onPress: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixColors.Darker,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("A fita LED", style = PixTypography.sectionTitle, color = PixColors.Cyan)

            Text(
                "Aperte como no controle de verdade: a geladeira acende na hora. O que ficar aceso é o que ela mostra quando não está acontecendo nada.",
                style = PixTypography.caption,
                color = PixColors.Gray300,
            )

            if (led.profiles.isEmpty()) {
                Text(
                    "Nenhum controle no catálogo ainda. Um controle novo se mapeia uma vez, no painel, e vale para toda fita igual.",
                    style = PixTypography.bodyMuted,
                    color = PixColors.Gray300,
                )
            } else {
                Text("Qual controle veio com esta fita?", style = PixTypography.inputLabel, color = PixColors.Gray100)

                led.profiles.forEach { profile ->
                    SensorCard(
                        icon = if (profile.id == led.profileId) AppIconType.Check else AppIconType.Bulb,
                        label = "${profile.keys.size} teclas mapeadas",
                        value = profile.name,
                        accent = if (profile.id == led.profileId) PixColors.Cyan else PixColors.Gray500,
                        onClick = { onChooseProfile(profile.id) },
                    )
                }
            }

            if (led.profileId != null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IrRemote(
                        available = led.availableKeys,
                        onPress = { onPress(it.slug) },
                        enabled = online,
                        busy = pressing,
                        picked = picked,
                        lastSent = lastSent,
                    )
                }

                Text(
                    "Brilho, tons e os programas piscantes também funcionam — só não servem de repouso.",
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )

                FridgeLamp(
                    light = FridgeLight.from(picked ?: led.color),
                    label = if (picked != null) "Vai ficar assim" else "Está assim",
                    hint = led.colors.firstOrNull { it.value == (picked ?: led.color) }?.label,
                )
            }

            if (!online) {
                Text(
                    "A geladeira está fora do ar: as cores só se escolhem com ela ligada, porque é nela que você vê.",
                    style = PixTypography.bodyMuted,
                    color = PixColors.Yellow,
                )
            }

            PixelButton(
                text = "Salvar esta cor",
                onClick = onSave,
                enabled = picked != null,
                modifier = Modifier.fillMaxWidth(),
            )

            PixelButton(
                text = "Fechar",
                onClick = onDismiss,
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
