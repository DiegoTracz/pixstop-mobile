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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.Department
import com.pixstop.mobile.domain.model.TeamMember
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.TeamViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Área do gestor: a verba do departamento e a distribuição de pixels.
 *
 * Distribuir é tudo ou nada no servidor, então a tela pode dizer o resultado
 * sem ressalva — ou todos receberam, ou ninguém recebeu.
 */
@Composable
fun TeamScreen(
    onBack: () -> Unit,
    viewModel: TeamViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val department = state.selected

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(
            title = department?.name ?: "Meu time",
            // Com mais de um departamento, voltar significa "escolher outro".
            onBack = {
                if (department != null && state.departments.size > 1) {
                    viewModel.back()
                } else {
                    onBack()
                }
            },
        )

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelLoader()
            }

            state.departments.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelEmptyState(
                    message = state.error ?: "Você não gere nenhum departamento.",
                    isError = state.error != null,
                )
            }

            department == null -> DepartmentList(
                departments = state.departments,
                onSelect = viewModel::select,
            )

            else -> {
                BudgetHeader(department)

                MembersList(
                    members = state.members,
                    chosen = state.chosen,
                    allChosen = state.allChosen,
                    onToggle = viewModel::toggleMember,
                    onToggleAll = viewModel::toggleAll,
                    modifier = Modifier.weight(1f),
                )

                DistributeBar(
                    amountText = state.amountText,
                    reason = state.reason,
                    chosenCount = state.chosen.size,
                    total = state.totalToDistribute,
                    exceedsBudget = state.exceedsBudget,
                    canDistribute = state.canDistribute,
                    isDistributing = state.isDistributing,
                    message = state.message,
                    error = state.error,
                    onAmountChange = viewModel::onAmountChange,
                    onReasonChange = viewModel::onReasonChange,
                    onDistribute = viewModel::distribute,
                )
            }
        }
    }
}

@Composable
private fun DepartmentList(departments: List<Department>, onSelect: (Department) -> Unit) {
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
                    .clickable { onSelect(department) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = department.name, style = PixTypography.sectionTitle, color = PixColors.Cyan)

                Text(
                    text = "${department.membersCount} pessoas",
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PixelCoin(size = 14.dp)

                    Text(
                        text = "${department.pixelBalance} na verba",
                        style = PixTypography.caption,
                        color = PixColors.Yellow,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetHeader(department: Department) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .border(2.dp, PixColors.Yellow)
            .background(PixColors.Darker)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelCoin(size = 20.dp)

        Text(
            text = department.pixelBalance.toString(),
            style = PixTypography.sectionTitle,
            color = PixColors.Yellow,
        )

        Text(text = "na verba do time", style = PixTypography.bodySecondary)
    }
}

@Composable
private fun MembersList(
    members: List<TeamMember>,
    chosen: Set<Long>,
    allChosen: Boolean,
    onToggle: (Long) -> Unit,
    onToggleAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Quem recebe", style = PixTypography.sectionTitle, color = PixColors.Cyan)

            PixelButton(
                text = if (allChosen) "Limpar" else "Todos",
                onClick = onToggleAll,
                variant = PixelButtonVariant.Secondary,
                buttonSize = PixelButtonSize.Small,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(members, key = { it.userId }) { member ->
                MemberRow(
                    member = member,
                    checked = member.userId in chosen,
                    onToggle = { onToggle(member.userId) },
                )
            }
        }
    }
}

@Composable
private fun MemberRow(member: TeamMember, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (checked) PixColors.Cyan else PixColors.Gray700)
            .background(if (checked) PixColors.CyanAlpha10 else PixColors.Darker)
            // Inativo continua na lista, apagado: sumir com a pessoa faria
            // parecer que ela saiu da empresa.
            .clickable(enabled = member.isActive, onClick = onToggle)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, if (checked) PixColors.Cyan else PixColors.Gray500)
                .background(if (checked) PixColors.Cyan else PixColors.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                AppIcon(
                    icon = AppIconType.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = PixColors.Dark,
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = member.name,
                color = if (member.isActive) PixColors.Gray100 else PixColors.Gray500,
            )

            Text(
                text = when {
                    !member.isActive -> "sem acesso"
                    member.isManager -> "gestor"
                    else -> member.email.orEmpty()
                },
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )
        }

        if (member.level > 0) {
            Text(
                text = "Nv. ${member.level}",
                style = PixTypography.caption,
                color = PixColors.Cyan,
                modifier = Modifier.border(1.dp, PixColors.Cyan).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelCoin(size = 12.dp)

            Text(text = member.pixelAvailable.toString(), style = PixTypography.caption, color = PixColors.Yellow)
        }
    }
}

@Composable
private fun DistributeBar(
    amountText: String,
    reason: String,
    chosenCount: Int,
    total: Int,
    exceedsBudget: Boolean,
    canDistribute: Boolean,
    isDistributing: Boolean,
    message: String?,
    error: String?,
    onAmountChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onDistribute: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixColors.Darker)
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PixelInput(
            value = amountText,
            onValueChange = onAmountChange,
            label = "Pixels para cada um",
            placeholder = "0",
            enabled = !isDistributing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        PixelInput(
            value = reason,
            onValueChange = onReasonChange,
            label = "Motivo (opcional)",
            placeholder = "Meta batida em setembro",
            enabled = !isDistributing,
            modifier = Modifier.fillMaxWidth(),
        )

        if (chosenCount > 0 && total > 0) {
            Text(
                text = "$total pixels no total, para $chosenCount " +
                    if (chosenCount == 1) "pessoa" else "pessoas",
                style = PixTypography.caption,
                color = if (exceedsBudget) PixColors.Pink else PixColors.Gray400,
            )
        }

        if (exceedsBudget) {
            Text(text = "A verba do time não cobre esse total.", style = PixTypography.errorText)
        }

        error?.let {
            Text(text = it, style = PixTypography.errorText)
        }

        message?.let {
            Text(text = it, style = PixTypography.caption, color = PixColors.Green)
        }

        PixelButton(
            text = "Distribuir",
            onClick = onDistribute,
            enabled = canDistribute,
            isLoading = isDistributing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
