package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.Category
import com.pixstop.mobile.domain.model.Product
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.ApplianceSwitcherSheet
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.DiscountTag
import com.pixstop.mobile.ui.components.PixelBalance
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelPrice
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ShopViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Vitrine da empresa.
 *
 * Saldo no topo, busca, régua de categorias e a lista. O estoque mostrado já
 * desconta o que está reservado no carrinho de outras pessoas.
 */
@Composable
fun ShopScreen(
    onProductClick: (Long) -> Unit,
    onAddToCart: (Long, Long?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.products.lastIndex - 3
        }
    }

    LaunchedEffect(shouldLoadMore, state.hasNextPage) {
        if (shouldLoadMore && state.hasNextPage) {
            viewModel.loadMore()
        }
    }

    if (state.choosingAppliance) {
        ApplianceSwitcherSheet(
            appliances = state.appliances.appliances,
            currentId = state.appliances.currentId,
            // Sem porta escolhida a vitrine não sabe o que mostrar, e fechar
            // deixaria a pessoa numa tela que não responde nada.
            dismissible = state.appliance != null,
            onSelect = { viewModel.onApplianceSelected(it.id) },
            onDismiss = viewModel::dismissApplianceChoice,
        )
    }

    Column(modifier = modifier.fillMaxSize().background(PixColors.Dark)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "Loja", style = PixTypography.sectionTitle, color = PixColors.Cyan)

                // Com uma geladeira só não há o que mostrar: ela é a resposta.
                state.appliance?.takeIf { state.appliances.hasChoice }?.let { appliance ->
                    Row(
                        modifier = Modifier.clickable(onClick = viewModel::openApplianceChoice),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = appliance.name, style = PixTypography.bodyMuted, color = PixColors.Green)
                        Text(text = "· trocar", style = PixTypography.bodyMuted)
                    }
                }
            }

            PixelBalance(pixels = state.wallet.available)
        }

        PixelInput(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = "",
            placeholder = "Buscar produto",
            leadingIcon = {
                AppIcon(
                    icon = AppIconType.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PixColors.Gray400,
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )

        if (state.categories.isNotEmpty()) {
            CategoryStrip(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelect = viewModel::onCategorySelected,
            )
        }

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelLoader()
            }

            state.error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error.orEmpty(), style = PixTypography.errorText)
            }

            state.isEmpty -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (state.isFiltered) "Nada encontrado com esse filtro." else "A vitrine ainda está vazia.",
                    style = PixTypography.bodyMuted,
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.products, key = { it.id }) { product ->
                    ProductRow(
                        product = product,
                        onClick = { onProductClick(product.id) },
                        // A geladeira vai junto: é dela que o produto sai, e
                        // é ela que o carrinho passa a guardar.
                        onAdd = { onAddToCart(product.id, state.appliance?.id) },
                    )
                }

                if (state.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PixColors.Cyan)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Régua de categorias. Tocar na já escolhida a desmarca.
 */
@Composable
private fun CategoryStrip(
    categories: List<Category>,
    selected: Long?,
    onSelect: (Long) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = category.id == selected

            Text(
                text = category.name.uppercase(),
                style = PixTypography.badgeText,
                color = if (isSelected) PixColors.Dark else PixColors.Cyan,
                modifier = Modifier
                    .border(2.dp, PixColors.Cyan)
                    .background(if (isSelected) PixColors.Cyan else PixColors.Transparent)
                    .clickable { onSelect(category.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ProductRow(product: Product, onClick: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (product.isSoldOut) PixColors.Gray700 else PixColors.Gray600)
            .background(PixColors.Darker)
            .clickable(enabled = !product.isSoldOut, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductThumb(product = product)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = product.name,
                color = if (product.isSoldOut) PixColors.Gray500 else PixColors.Gray100,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            product.categoryName?.let {
                Text(text = it, style = PixTypography.caption, color = PixColors.Gray400)
            }

            PixelPrice(
                priceMoney = product.priceMoneyDiscounted,
                priceMoneyOriginal = product.priceMoney,
                pricePixels = product.pricePixelsDiscounted,
            )

            Text(
                text = if (product.isSoldOut) "Esgotado" else "${product.available} disponíveis",
                style = PixTypography.caption,
                color = if (product.isSoldOut) PixColors.Pink else PixColors.Gray400,
            )
        }

        // Na vitrine só dava para abrir o produto: para pôr no carrinho era
        // preciso entrar nele e voltar. Quem já sabe o que quer levar não
        // deveria pagar esse pedágio.
        AddButton(enabled = !product.isSoldOut, onClick = onAdd)
    }
}

/** O mesmo quadrado de adicionar dos cartões do início. */
@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (enabled) PixColors.Cyan else PixColors.Gray700)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            icon = AppIconType.CartAdd,
            contentDescription = if (enabled) "Adicionar ao carrinho" else "Esgotado",
            modifier = Modifier.size(20.dp),
            tint = if (enabled) PixColors.Dark else PixColors.Gray500,
        )
    }
}

/**
 * Miniatura do produto.
 *
 * Ainda é a inicial num quadrado: mostrar a foto pede uma biblioteca de
 * carregamento de imagem que o app não tem.
 */
@Composable
private fun ProductThumb(product: Product) {
    Box(modifier = Modifier.size(56.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(2.dp, PixColors.Gray700)
                .background(PixColors.Gray800),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = product.name.take(1).uppercase(),
                style = PixTypography.sectionTitle,
                color = PixColors.Gray500,
            )
        }

        if (product.hasDiscount) {
            DiscountTag(
                percentage = product.discountPercentage,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}
