package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.ChatMessage
import com.pixstop.mobile.domain.model.ChatRole
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.domain.model.ToolCard
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCameraScreen
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelMascot
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.rememberPhotoPicker
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.viewmodel.AssistantUiState
import com.pixstop.mobile.ui.viewmodel.AssistantViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * O assistente de IA no celular (ASSISTENTE_IA.md, A8): pergunta, foto do
 * produto pela câmera e os mesmos cards com Confirmar e Cancelar do painel.
 */
@Composable
fun AssistantScreen(
    onBack: () -> Unit,
    viewModel: AssistantViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var cameraOpen by remember { mutableStateOf(false) }
    val pickFromGallery = rememberPhotoPicker { photo -> viewModel.onPhoto(photo) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isSending) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size)
    }

    if (cameraOpen) {
        PixelCameraScreen(
            onPhoto = { photo ->
                cameraOpen = false
                viewModel.onPhoto(photo)
            },
            onDismiss = { cameraOpen = false },
            title = "FOTO DO PRODUTO",
            instruction = "O produto inteiro, com o rótulo de frente.",
            startWithFront = false,
            squareFrame = true,
        )

        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PixColors.Dark)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        PixelScreenTopBar(
            title = "Assistente",
            onBack = onBack,
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Conversas",
                        color = PixColors.Cyan,
                        modifier = Modifier.clickable(onClick = viewModel::toggleHistory),
                    )
                    Box(Modifier.clickable(onClick = viewModel::newConversation)) {
                        AppIcon(icon = AppIconType.Add, contentDescription = "Nova conversa", tint = PixColors.Cyan, modifier = Modifier.size(22.dp))
                    }
                }
            },
        )

        if (state.showHistory) {
            ConversationList(state, onOpen = viewModel::open, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.isEmpty) {
                    item { Welcome(onSuggestion = viewModel::send) }
                }

                items(state.messages, key = { it.id }) { message ->
                    ChatItem(message, busy = state.isSending, onDecide = viewModel::decide)
                }

                if (state.isSending) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PixelMascot(state = MascotState.Loading, size = 28.dp, decorative = true)
                            Text("Pensando…", color = PixColors.Gray400)
                        }
                    }
                }
            }
        }

        Composer(
            state = state,
            onDraft = viewModel::onDraft,
            onSend = { viewModel.send() },
            onCamera = { cameraOpen = true },
            onGallery = pickFromGallery,
            onRemovePhoto = viewModel::removePhoto,
        )
    }
}

@Composable
private fun Welcome(onSuggestion: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixelMascot(state = MascotState.Idle, size = 72.dp, decorative = true)
        Text(
            "Pergunte sobre vendas, estoque, clientes e gravações, ou fotografe um produto para cadastrar.",
            color = PixColors.Gray300,
            textAlign = TextAlign.Center,
        )
        AssistantUiState.SUGGESTIONS.forEach { suggestion ->
            Text(
                text = suggestion,
                color = PixColors.Cyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, PixColors.Gray700, RoundedCornerShape(16.dp))
                    .clickable { onSuggestion(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ConversationList(state: AssistantUiState, onOpen: (Long) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.conversations.isEmpty()) {
            item { Text("Nenhuma conversa ainda.", color = PixColors.Gray400) }
        }

        items(state.conversations, key = { it.id }) { conversation ->
            Text(
                text = conversation.title,
                color = if (conversation.id == state.conversationId) PixColors.Cyan else PixColors.White,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PixColors.Gray900)
                    .clickable { onOpen(conversation.id) }
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun ChatItem(message: ChatMessage, busy: Boolean, onDecide: (String, String, Boolean) -> Unit) {
    when (message.role) {
        ChatRole.User -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column(horizontalAlignment = Alignment.End) {
                if (message.photos > 0) {
                    Text("📷 ${message.photos} foto(s)", color = PixColors.Gray400)
                }
                Text(
                    text = message.text,
                    color = PixColors.White,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PixColors.Blue)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        ChatRole.Confirmation -> Text(
            text = message.text,
            color = PixColors.Gray400,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        ChatRole.Tool -> message.card?.let { ToolCardView(it, busy, onDecide) }

        ChatRole.Assistant -> Text(
            text = message.text,
            color = PixColors.White,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PixColors.Gray900)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ToolCardView(card: ToolCard, busy: Boolean, onDecide: (String, String, Boolean) -> Unit) {
    val borderColor = when {
        !card.success -> PixColors.Pink
        card.awaitingConfirmation -> PixColors.Blue
        else -> PixColors.Gray700
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(PixColors.Gray900)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(card.title.uppercase(), color = PixColors.Gray400)
            when (card.decision) {
                "confirmed" -> Text("Confirmado", color = PixColors.Green)
                "canceled" -> Text("Cancelado", color = PixColors.Gray400)
            }
        }

        Text(card.message, color = if (card.success) PixColors.White else PixColors.Pink)

        card.fields.forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, color = PixColors.Gray400, modifier = Modifier.widthIn(min = 96.dp, max = 120.dp))
                Text(value, color = PixColors.White)
            }
        }

        card.changes.forEach { Text(it, color = PixColors.White) }
        card.notes.forEach { Text("• $it", color = PixColors.Orange) }

        card.rows.forEach { row ->
            Column(Modifier.padding(top = 4.dp)) {
                Text(row.title, color = PixColors.White)
                if (row.detail.isNotBlank()) Text(row.detail, color = PixColors.Gray400)
            }
        }

        val token = card.confirmationToken
        if (card.awaitingConfirmation && token != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                PixelButton(
                    text = "Confirmar",
                    onClick = { onDecide(card.toolName, token, true) },
                    buttonSize = PixelButtonSize.Small,
                    enabled = !busy,
                )
                PixelButton(
                    text = "Cancelar",
                    onClick = { onDecide(card.toolName, token, false) },
                    variant = PixelButtonVariant.Secondary,
                    buttonSize = PixelButtonSize.Small,
                    enabled = !busy,
                )
            }
        }
    }
}

@Composable
private fun Composer(
    state: AssistantUiState,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemovePhoto: (Long) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(PixColors.Gray900).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.error?.let { Text(it, color = PixColors.Pink) }

        if (state.photos.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.photos.forEach { photo ->
                    val bitmap = remember(photo.id) { runCatching { photo.preview.decodeToImageBitmap() }.getOrNull() }
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).clickable { onRemovePhoto(photo.id) }) {
                        bitmap?.let { Image(it, contentDescription = "Foto anexada (toque para tirar)", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.clickable(enabled = state.canAddPhoto, onClick = onCamera)) {
                AppIcon(icon = AppIconType.Camera, contentDescription = "Fotografar produto", tint = if (state.canAddPhoto) PixColors.Cyan else PixColors.Gray700, modifier = Modifier.size(26.dp))
            }
            Text(
                text = "Galeria",
                color = if (state.canAddPhoto) PixColors.Cyan else PixColors.Gray700,
                modifier = Modifier.clickable(enabled = state.canAddPhoto, onClick = onGallery),
            )
            PixelInput(
                value = state.draft,
                onValueChange = onDraft,
                label = "",
                placeholder = if (state.isUploading) "Enviando a foto…" else "Pergunte ou peça algo…",
                singleLine = false,
                modifier = Modifier.weight(1f),
            )
            PixelButton(
                text = "Enviar",
                onClick = onSend,
                buttonSize = PixelButtonSize.Small,
                enabled = state.canSend,
            )
        }
    }
}
