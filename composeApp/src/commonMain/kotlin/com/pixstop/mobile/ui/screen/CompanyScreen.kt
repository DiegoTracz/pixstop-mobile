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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.CompanyDashboard
import com.pixstop.mobile.domain.model.CompanyMember
import com.pixstop.mobile.domain.model.CompanyOrder
import com.pixstop.mobile.domain.model.CompanyRole
import com.pixstop.mobile.domain.model.Department
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CompanyAction
import com.pixstop.mobile.ui.viewmodel.CompanySection
import com.pixstop.mobile.ui.viewmodel.CompanyViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Painel do administrador da empresa.
 *
 * Nenhuma ação daqui acontece com um toque só: todas mexem em dinheiro, em
 * pixels ou no acesso de alguém, e passam por uma confirmação que explica o
 * que vai acontecer.
 */
@Composable
fun CompanyScreen(
    onBack: () -> Unit,
    viewModel: CompanyViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Empresa", onBack = onBack)

        SectionTabs(selected = state.section, onSelect = viewModel::openSection)

        state.message?.let {
            Banner(text = it, color = PixColors.Green, onDismiss = viewModel::dismissMessage)
        }

        state.error?.takeIf { state.pending == null }?.let {
            Banner(text = it, color = PixColors.Pink, onDismiss = viewModel::dismissMessage)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PixelLoader()
                }

                else -> when (state.section) {
                    CompanySection.Summary -> state.dashboard?.let { SummarySection(it) }

                    CompanySection.Orders -> OrdersSection(
                        orders = state.orders,
                        onApprove = { viewModel.ask(CompanyAction.ApproveOrder(it)) },
                        onCancel = { viewModel.ask(CompanyAction.CancelOrder(it)) },
                    )

                    CompanySection.People -> PeopleSection(
                        members = state.members,
                        chosen = state.chosen,
                        onToggleChosen = viewModel::toggleChosen,
                        onDistribute = { viewModel.ask(CompanyAction.DistributePixels(state.chosenMembers)) },
                        onCredit = { viewModel.ask(CompanyAction.AdjustBalance(it, credit = true)) },
                        onDebit = { viewModel.ask(CompanyAction.AdjustBalance(it, credit = false)) },
                        onToggleAccess = { viewModel.ask(CompanyAction.ToggleUser(it)) },
                    )

                    CompanySection.Teams -> TeamsSection(
                        departments = state.departments,
                        onAllocate = { viewModel.ask(CompanyAction.AllocatePixels(it)) },
                    )
                }
            }
        }
    }

    state.pending?.let { action ->
        CompanyActionDialog(
            action = action,
            state = state,
            onAmountChange = viewModel::onAmountChange,
            onReasonChange = viewModel::onReasonChange,
            onConfirm = viewModel::confirm,
            onDismiss = viewModel::dismiss,
        )
    }
}

@Composable
private fun SectionTabs(selected: CompanySection, onSelect: (CompanySection) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompanySection.entries.forEach { section ->
            val isSelected = section == selected

            Text(
                text = section.label.uppercase(),
                style = PixTypography.badgeText,
                color = if (isSelected) PixColors.Dark else PixColors.Cyan,
                modifier = Modifier
                    .border(2.dp, PixColors.Cyan)
                    .background(if (isSelected) PixColors.Cyan else PixColors.Transparent)
                    .clickable { onSelect(section) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Banner(text: String, color: androidx.compose.ui.graphics.Color, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixColors.Darker)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(text = text, style = PixTypography.caption, color = color)
    }
}

/**
 * Resumo: o que entrou, o que sobrou e o que a empresa ainda tem para dar.
 */
@Composable
private fun SummarySection(dashboard: CompanyDashboard) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(title = "Vendas nos últimos ${dashboard.periodDays} dias") {
                Line("Pedidos", dashboard.orders.toString())
                Line("Receita", formatMoney(dashboard.revenue))
                Line("Taxas", "−${formatMoney(dashboard.fees)}", PixColors.Pink)
                Line("Receita líquida", formatMoney(dashboard.netRevenue), PixColors.Green)
                Line("Lucro", formatMoney(dashboard.profit), PixColors.Green)
                Line("Pixels resgatados", dashboard.pixelsRedeemed.toString(), PixColors.Yellow)
            }
        }

        item {
            Card(title = "O que a empresa tem") {
                Line("Pixels no cofre", dashboard.corporatePixels.toString(), PixColors.Yellow)
                Line("Pixels nos times", dashboard.departmentPixels.toString(), PixColors.Yellow)
                Line("Saldo distribuído", formatMoney(dashboard.companyBalance), PixColors.Green)
                Line("Pessoas ativas", "${dashboard.membersActive} de ${dashboard.membersTotal}")
            }
        }

        if (dashboard.topProducts.isNotEmpty()) {
            item {
                Card(title = "Mais vendidos") {
                    dashboard.topProducts.forEach {
                        Line("${it.quantity}× ${it.name}", formatMoney(it.revenue))
                    }
                }
            }
        }

        if (dashboard.lowStock.isNotEmpty()) {
            item {
                Card(title = "Estoque baixo") {
                    dashboard.lowStock.forEach {
                        Line(
                            label = it.name,
                            value = if (it.stock == 0) "esgotado" else "${it.stock} restantes",
                            color = if (it.stock == 0) PixColors.Pink else PixColors.Yellow,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersSection(
    orders: List<CompanyOrder>,
    onApprove: (CompanyOrder) -> Unit,
    onCancel: (CompanyOrder) -> Unit,
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Nenhum pedido ainda.", style = PixTypography.bodyMuted)
        }

        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(orders, key = { it.id }) { order ->
            val statusColor = when (order.status) {
                OrderStatus.Paid, OrderStatus.Delivered -> PixColors.Green
                OrderStatus.Canceled -> PixColors.Pink
                else -> PixColors.Yellow
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, PixColors.Gray600)
                    .background(PixColors.Darker)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = order.transactionId ?: "Pedido ${order.id}",
                        style = PixTypography.caption,
                        color = PixColors.Gray400,
                    )

                    Text(text = order.statusLabel, style = PixTypography.badgeText, color = statusColor)
                }

                Text(text = order.buyerName.orEmpty(), color = PixColors.Gray100)

                Line("Líquido para a empresa", formatMoney(order.netRevenue), PixColors.Green)

                order.createdAt?.let {
                    Text(text = it, style = PixTypography.caption, color = PixColors.Gray500)
                }

                // Só o que ainda está aberto aceita ação.
                if (order.isOpen) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PixelButton(
                            text = "Aprovar",
                            onClick = { onApprove(order) },
                            buttonSize = PixelButtonSize.Small,
                        )

                        PixelButton(
                            text = "Cancelar",
                            onClick = { onCancel(order) },
                            variant = PixelButtonVariant.Destructive,
                            buttonSize = PixelButtonSize.Small,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleSection(
    members: List<CompanyMember>,
    chosen: Set<Long>,
    onToggleChosen: (Long) -> Unit,
    onDistribute: () -> Unit,
    onCredit: (CompanyMember) -> Unit,
    onDebit: (CompanyMember) -> Unit,
    onToggleAccess: (CompanyMember) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (chosen.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${chosen.size} " + if (chosen.size == 1) "escolhido" else "escolhidos",
                    style = PixTypography.caption,
                    color = PixColors.Cyan,
                )

                PixelButton(
                    text = "Dar pixels",
                    onClick = onDistribute,
                    buttonSize = PixelButtonSize.Small,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(members, key = { it.id }) { member ->
                MemberCard(
                    member = member,
                    checked = member.id in chosen,
                    onToggleChosen = { onToggleChosen(member.id) },
                    onCredit = { onCredit(member) },
                    onDebit = { onDebit(member) },
                    onToggleAccess = { onToggleAccess(member) },
                )
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: CompanyMember,
    checked: Boolean,
    onToggleChosen: () -> Unit,
    onCredit: () -> Unit,
    onDebit: () -> Unit,
    onToggleAccess: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (checked) PixColors.Cyan else PixColors.Gray600)
            .background(if (checked) PixColors.CyanAlpha10 else PixColors.Darker)
            .clickable(enabled = member.isActive, onClick = onToggleChosen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    color = if (member.isActive) PixColors.Gray100 else PixColors.Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = when {
                        !member.isActive -> "sem acesso"
                        member.role == CompanyRole.Admin -> "administrador"
                        member.role == CompanyRole.Manager -> "gestor"
                        else -> member.email.orEmpty()
                    },
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = formatMoney(member.balance), color = PixColors.Green)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    PixelCoin(size = 12.dp)

                    Text(
                        text = member.pixelAvailable.toString(),
                        style = PixTypography.caption,
                        color = PixColors.Yellow,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton(text = "+ Saldo", onClick = onCredit, buttonSize = PixelButtonSize.Small)

            PixelButton(
                text = "− Saldo",
                onClick = onDebit,
                variant = PixelButtonVariant.Secondary,
                buttonSize = PixelButtonSize.Small,
            )

            PixelButton(
                text = if (member.isActive) "Desativar" else "Reativar",
                onClick = onToggleAccess,
                variant = if (member.isActive) PixelButtonVariant.Destructive else PixelButtonVariant.Secondary,
                buttonSize = PixelButtonSize.Small,
            )
        }
    }
}

@Composable
private fun TeamsSection(departments: List<Department>, onAllocate: (Department) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(departments, key = { it.id }) { department ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, PixColors.Gray600)
                    .background(PixColors.Darker)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = department.name, style = PixTypography.sectionTitle, color = PixColors.Cyan)

                Text(
                    text = "${department.membersCount} pessoas",
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PixelCoin(size = 14.dp)

                        Text(
                            text = "${department.pixelBalance} na verba",
                            style = PixTypography.caption,
                            color = PixColors.Yellow,
                        )
                    }

                    PixelButton(
                        text = "Alocar",
                        onClick = { onAllocate(department) },
                        buttonSize = PixelButtonSize.Small,
                    )
                }
            }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Gray600)
            .background(PixColors.Darker)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = PixTypography.sectionTitle, color = PixColors.Cyan)

        content()
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = PixColors.Gray100,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = PixTypography.bodySecondary,
            color = PixColors.Gray300,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(text = value, color = color, modifier = Modifier.padding(start = 8.dp))
    }
}
