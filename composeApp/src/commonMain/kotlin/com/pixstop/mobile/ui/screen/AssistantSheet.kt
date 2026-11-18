package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import com.pixstop.mobile.ui.theme.PixTypography
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
import com.pixstop.mobile.ui.components.PixelMascot
import com.pixstop.mobile.ui.components.rememberPhotoPicker
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.viewmodel.AssistantUiState
import com.pixstop.mobile.ui.viewmodel.AssistantViewModel
import org.koin.compose.viewmodel.koinViewModel

/** As perguntas prontas da conversa vazia, cada uma com o ícone do assunto. */
private val SUGGESTION_ICONS = listOf(AppIconType.Person, AppIconType.Store, AppIconType.Box, AppIconType.Bulb)

/**
 * O assistente de IA no celular (ASSISTENTE_IA.md, A8), aberto pelo mascote
 * flutuante da Home numa folha de baixo para cima: pergunta, foto do produto
 * e os mesmos cards com Confirmar e Cancelar do painel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSheet(
    onDismiss: () -> Unit,
    viewModel: AssistantViewModel = koinViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PixColors.Dark,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        AssistantChat(onClose = onDismiss, viewModel = viewModel, modifier = Modifier.fillMaxHeight(0.94f))
    }
}

@Composable
private fun AssistantChat(onClose: () -> Unit, viewModel: AssistantViewModel, modifier: Modifier = Modifier) {
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

    Column(modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
        Header(
            showingHistory = state.showHistory,
            thinking = state.isSending || state.isUploading,
            onBack = { if (state.showHistory) viewModel.toggleHistory() else onClose() },
            onHistory = viewModel::toggleHistory,
        )

        if (state.showHistory) {
            ConversationList(
                state,
                onOpen = viewModel::open,
                onNew = viewModel::newConversation,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
            ) {
                if (state.isEmpty) {
                    item { Suggestions(onSuggestion = viewModel::send) }
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

/** Voltar à esquerda, o mascote no meio e as conversas à direita, como no Mercado Pago. */
@Composable
private fun Header(showingHistory: Boolean, thinking: Boolean, onBack: () -> Unit, onHistory: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareIconButton(AppIconType.ArrowBack, if (showingHistory) "Voltar à conversa" else "Fechar o assistente", onBack)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (showingHistory) {
                Text("Conversas", style = PixTypography.sectionTitle, color = PixColors.Gray100)
            } else {
                PixelMascot(state = if (thinking) MascotState.Loading else MascotState.Idle, size = 36.dp, decorative = true)
            }
        }
        if (showingHistory) {
            Spacer(Modifier.size(48.dp))
        } else {
            SquareIconButton(AppIconType.Menu, "Conversas anteriores", onHistory)
        }
    }
}

@Composable
private fun SquareIconButton(icon: AppIconType, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PixColors.CyanAlpha10)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon = icon, contentDescription = description, tint = PixColors.Cyan, modifier = Modifier.size(22.dp))
    }
}

/** A conversa vazia: as perguntas prontas em lista, logo acima do campo. */
@Composable
private fun Suggestions(onSuggestion: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Pergunte sobre vendas, estoque, clientes e gravações, ou fotografe um produto para cadastrar.",
            color = PixColors.Gray400,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        AssistantUiState.SUGGESTIONS.forEachIndexed { index, suggestion ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSuggestion(suggestion) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(PixColors.CyanAlpha10),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(icon = SUGGESTION_ICONS[index % SUGGESTION_ICONS.size], contentDescription = null, tint = PixColors.Cyan, modifier = Modifier.size(22.dp))
                }
                Text(suggestion, color = PixColors.Gray100, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun ConversationList(state: AssistantUiState, onOpen: (Long) -> Unit, onNew: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PixColors.CyanAlpha10)
                    .clickable(onClick = onNew)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppIcon(icon = AppIconType.Add, contentDescription = null, tint = PixColors.Cyan, modifier = Modifier.size(20.dp))
                Text("Nova conversa", color = PixColors.Cyan)
            }
        }

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
                    .padding(14.dp),
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

/**
 * O campo, como o do Mercado Pago: uma pílula com o "+" da galeria, o texto,
 * a câmera e o botão redondo de enviar.
 */
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
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.error?.let { Text(it, color = PixColors.Pink, modifier = Modifier.padding(horizontal = 8.dp)) }

        if (state.photos.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                state.photos.forEach { photo ->
                    val bitmap = remember(photo.id) { runCatching { photo.preview.decodeToImageBitmap() }.getOrNull() }
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).clickable { onRemovePhoto(photo.id) }) {
                        bitmap?.let { Image(it, contentDescription = "Foto anexada (toque para tirar)", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        AppIcon(
                            icon = AppIconType.Close,
                            contentDescription = null,
                            tint = PixColors.White,
                            modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(16.dp).clip(CircleShape).background(PixColors.Gray800.copy(alpha = 0.8f)),
                        )
                    }
                }
            }
        }

        val pill = RoundedCornerShape(32.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(4.dp, pill)
                .clip(pill)
                .background(PixColors.Darker)
                .border(1.dp, PixColors.Gray800, pill)
                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(enabled = state.canAddPhoto, onClick = onGallery),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon = AppIconType.Add, contentDescription = "Foto da galeria", tint = if (state.canAddPhoto) PixColors.Gray200 else PixColors.Gray700, modifier = Modifier.size(24.dp))
            }

            Box(Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                if (state.draft.isEmpty()) {
                    Text(
                        if (state.isUploading) "Enviando a foto…" else "O que faço por você?",
                        color = PixColors.Gray500,
                        fontSize = 17.sp,
                    )
                }
                BasicTextField(
                    value = state.draft,
                    onValueChange = onDraft,
                    textStyle = TextStyle(color = PixColors.Gray100, fontSize = 17.sp),
                    cursorBrush = SolidColor(PixColors.Cyan),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(enabled = state.canAddPhoto, onClick = onCamera),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon = AppIconType.Camera, contentDescription = "Fotografar produto", tint = if (state.canAddPhoto) PixColors.Gray200 else PixColors.Gray700, modifier = Modifier.size(24.dp))
            }

            Box(
                Modifier
                    .padding(start = 4.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (state.canSend) PixColors.Blue else PixColors.Gray800)
                    .clickable(enabled = state.canSend, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    icon = AppIconType.ArrowBack,
                    contentDescription = "Enviar",
                    tint = if (state.canSend) PixColors.White else PixColors.Gray500,
                    modifier = Modifier.size(24.dp).rotate(90f),
                )
            }
        }
    }
}
