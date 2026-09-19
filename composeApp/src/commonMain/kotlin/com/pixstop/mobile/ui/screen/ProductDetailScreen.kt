package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.Product
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.DiscountTag
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelPrice
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CartViewModel
import com.pixstop.mobile.ui.viewmodel.ProductDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Detalhe do produto, com o seletor de quantidade e o botão de adicionar.
 *
 * O teto da quantidade é o estoque já descontado das reservas alheias —
 * deixar pedir mais só adiaria a recusa para o carrinho.
 */
@Composable
fun ProductDetailScreen(
    productId: Long,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(),
    cartViewModel: CartViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val cart by cartViewModel.uiState.collectAsState()
    var quantity by remember { mutableStateOf(1) }

    androidx.compose.runtime.LaunchedEffect(productId) {
        viewModel.load(productId)
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Produto", onBack = onBack)

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelLoader()
            }

            state.product == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error ?: "Produto não encontrado.", style = PixTypography.errorText)
            }

            else -> {
                val product = state.product!!

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProductHero(product)

                    Text(text = product.name, style = PixTypography.pageTitle, color = PixColors.Cyan)

                    product.categoryName?.let {
                        Text(text = it.uppercase(), style = PixTypography.caption, color = PixColors.Gray400)
                    }

                    PixelPrice(
                        priceMoney = product.priceMoneyDiscounted,
                        priceMoneyOriginal = product.priceMoney,
                        pricePixels = product.pricePixelsDiscounted,
                    )

                    Text(
                        text = if (product.isSoldOut) "Esgotado" else "${product.available} disponíveis",
                        style = PixTypography.caption,
                        color = if (product.isSoldOut) PixColors.Pink else PixColors.Green,
                    )

                    product.description?.let {
                        Text(text = it, style = PixTypography.bodySecondary)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixColors.Darker)
                        .navigationBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    cart.error?.let {
                        Text(text = it, style = PixTypography.errorText)
                    }

                    if (!product.isSoldOut) {
                        QuantityStepper(
                            quantity = quantity,
                            max = product.available,
                            onChange = { quantity = it },
                        )
                    }

                    PixelButton(
                        text = if (product.isSoldOut) "Esgotado" else "Adicionar ao carrinho",
                        onClick = { cartViewModel.add(product.id, quantity, onAdded = onOpenCart) },
                        enabled = !product.isSoldOut,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHero(product: Product) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, PixColors.Gray700)
                .background(PixColors.Gray800),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = product.name.take(1).uppercase(),
                style = PixTypography.pageTitle,
                color = PixColors.Gray500,
            )
        }

        if (product.hasDiscount) {
            DiscountTag(
                percentage = product.discountPercentage,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            )
        }
    }
}

/**
 * Menos, número, mais — com alvos de toque de 48dp.
 */
@Composable
private fun QuantityStepper(quantity: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            icon = AppIconType.Remove,
            description = "Menos um",
            enabled = quantity > 1,
            onClick = { onChange(quantity - 1) },
        )

        Text(text = quantity.toString(), style = PixTypography.sectionTitle, color = PixColors.Gray100)

        StepButton(
            icon = AppIconType.Add,
            description = "Mais um",
            enabled = quantity < max,
            onClick = { onChange(quantity + 1) },
        )
    }
}

@Composable
private fun StepButton(
    icon: AppIconType,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, if (enabled) PixColors.Cyan else PixColors.Gray700)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            icon = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) PixColors.Cyan else PixColors.Gray700,
        )
    }
}
