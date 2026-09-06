package com.pixstop.mobile.ui.screen

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.AccountUser
import com.pixstop.mobile.domain.model.ActiveCompany
import com.pixstop.mobile.domain.model.Category
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.PixelWallet
import com.pixstop.mobile.domain.model.Product
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.DiscountTag
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.XpBar
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.HomeFeedUiState

/**
 * Tela de início.
 *
 * A ordem das seções segue o que uma despensa de empresa realmente é: primeiro
 * o que está pendente, depois o que a pessoa já comprou antes — quase toda
 * compra aqui é repetição —, e só então a descoberta.
 */
@Composable
fun HomeFeed(
    state: HomeFeedUiState,
    user: AccountUser?,
    company: ActiveCompany?,
    greeting: String,
    onOpenOrder: (Long) -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenCategory: (Long) -> Unit,
    onOpenShop: () -> Unit,
    onOpenPixels: () -> Unit,
    onOpenProgress: () -> Unit,
    onAddToCart: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PixColors.Cyan)
        }

        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Greeting(greeting = greeting, name = user?.firstName, company = company?.name)
        }

        item {
            WalletCard(
                wallet = state.wallet,
                balance = company?.balance ?: 0.0,
                onClick = onOpenPixels,
            )
        }

        // A barra de XP só existe onde a empresa ligou a progressão. Fica logo
        // abaixo da carteira: é a segunda coisa que a pessoa quer saber.
        company?.progression?.let { progression ->
            item {
                XpBar(
                    progression = progression,
                    onClick = onOpenProgress,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        state.openOrder?.let { order ->
            item {
                OpenOrderCard(order = order, onClick = { onOpenOrder(order.id) })
            }
        }

        if (state.buyAgain.isNotEmpty()) {
            item {
                ProductShelf(
                    title = "Comprar de novo",
                    subtitle = "O que você já levou antes",
                    products = state.buyAgain,
                    onProductClick = onOpenProduct,
                    onAddToCart = onAddToCart,
                )
            }
        }

        if (state.promotions.isNotEmpty()) {
            item {
                ProductShelf(
                    title = "Promoções",
                    subtitle = "Enquanto durar o estoque",
                    products = state.promotions,
                    onProductClick = onOpenProduct,
                    onAddToCart = onAddToCart,
                )
            }
        }

        if (state.categories.isNotEmpty()) {
            item {
                CategoryShelf(
                    categories = state.categories,
                    onCategoryClick = onOpenCategory,
                    onSeeAll = onOpenShop,
                )
            }
        }

        if (!state.hasContent) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.error ?: "A vitrine desta empresa ainda está vazia.",
                        style = if (state.error != null) PixTypography.errorText else PixTypography.bodyMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Greeting(greeting: String, name: String?, company: String?) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
            text = if (name != null) "$greeting, $name!" else greeting,
            style = PixTypography.pageTitle,
            color = PixColors.Cyan,
        )

        company?.let {
            Text(text = it, style = PixTypography.caption, color = PixColors.Gray400)
        }
    }
}

/**
 * Saldo e pixels — o que decide o que dá para comprar.
 *
 * É o primeiro cartão porque é o contexto de tudo o mais: sem saber quanto
 * tem, nenhum preço abaixo significa alguma coisa.
 */
@Composable
private fun WalletCard(wallet: PixelWallet, balance: Double, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(2.dp, PixColors.Cyan)
            .background(PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "SALDO", style = PixTypography.badgeText, color = PixColors.Gray400)

                Text(text = formatMoney(balance), style = PixTypography.pageTitle, color = PixColors.Green)
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = "PIXELS", style = PixTypography.badgeText, color = PixColors.Gray400)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PixelCoin(size = 18.dp)

                    Text(
                        text = wallet.available.toString(),
                        style = PixTypography.pageTitle,
                        color = PixColors.Yellow,
                    )
                }
            }
        }

        // O que está para vencer é a única informação da carteira que pede
        // ação; as outras duas só descrevem.
        if (wallet.expiringSoon > 0) {
            Text(
                text = "${wallet.expiringSoon} pixels vencem em breve",
                style = PixTypography.caption,
                color = PixColors.Pink,
            )
        }
    }
}

/**
 * O pedido que ainda espera algo: pagar ou retirar.
 */
@Composable
private fun OpenOrderCard(order: Order, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(2.dp, PixColors.Yellow)
            .background(PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(PixColors.Yellow),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                icon = AppIconType.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PixColors.Dark,
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = order.statusLabel, color = PixColors.Yellow)

            Text(
                text = if (order.pix != null) {
                    "Pague o PIX para liberar o produto"
                } else {
                    order.items.joinToString(", ") { "${it.quantity}× ${it.productName}" }
                },
                style = PixTypography.caption,
                color = PixColors.Gray400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AppIcon(
            icon = AppIconType.ChevronDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PixColors.Yellow,
        )
    }
}

/**
 * Uma prateleira horizontal de produtos.
 */
@Composable
private fun ProductShelf(
    title: String,
    subtitle: String,
    products: List<Product>,
    onProductClick: (Long) -> Unit,
    onAddToCart: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(text = title, style = PixTypography.sectionTitle, color = PixColors.Cyan)

            Text(text = subtitle, style = PixTypography.caption, color = PixColors.Gray400)
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product.id) },
                    onAdd = { onAddToCart(product.id) },
                )
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit, onAdd: () -> Unit) {
    Column(
        // Estreito de propósito: com um cartão inteiro e o começo do próximo à
        // vista, fica claro que a prateleira anda para o lado.
        modifier = Modifier
            .width(132.dp)
            .border(2.dp, if (product.isSoldOut) PixColors.Gray700 else PixColors.Gray600)
            .background(PixColors.Darker)
            .clickable(enabled = !product.isSoldOut, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Box(
                modifier = Modifier.fillMaxSize().background(PixColors.Gray800),
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
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }

            // No canto da imagem, como nos aplicativos de entrega: escrito por
            // extenso o rótulo não caberia na largura do cartão, e disputando
            // a linha com o preço quebraria o valor em duas.
            AddButton(
                enabled = !product.isSoldOut,
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }

        // Duas linhas sempre, mesmo com nome curto: sem isso os cartões da
        // prateleira ficariam de alturas diferentes, e a fileira desalinhada.
        Text(
            text = product.name,
            style = PixTypography.bodySecondary,
            color = if (product.isSoldOut) PixColors.Gray500 else PixColors.Gray100,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = formatMoney(product.priceMoneyDiscounted),
            style = PixTypography.sectionTitle,
            color = PixColors.Green,
            maxLines = 1,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelCoin(size = 12.dp)

            Text(
                text = product.pricePixelsDiscounted.toString(),
                style = PixTypography.caption,
                color = PixColors.Yellow,
            )
        }
    }
}

/**
 * Botão de adicionar ao carrinho.
 *
 * É só o ícone porque "Adicionar" escrito não cabe na largura de um cartão de
 * prateleira — quebraria a palavra em duas linhas. O alvo de toque continua
 * nos 40dp, e o rótulo vai na descrição para quem usa leitor de tela.
 */
@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
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

@Composable
private fun CategoryShelf(
    categories: List<Category>,
    onCategoryClick: (Long) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Categorias", style = PixTypography.sectionTitle, color = PixColors.Cyan)

            Text(
                text = "Ver tudo",
                style = PixTypography.caption,
                color = PixColors.Cyan,
                modifier = Modifier.clickable(onClick = onSeeAll).padding(8.dp),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .border(2.dp, PixColors.Gray600)
                        .background(PixColors.Darker)
                        .clickable { onCategoryClick(category.id) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(PixColors.CyanAlpha20),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = category.name.take(1).uppercase(),
                            style = PixTypography.sectionTitle,
                            color = PixColors.Cyan,
                        )
                    }

                    Text(
                        text = category.name,
                        style = PixTypography.caption,
                        color = PixColors.Gray100,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = "${category.productsCount} itens",
                        style = PixTypography.caption,
                        color = PixColors.Gray500,
                    )
                }
            }
        }
    }
}
