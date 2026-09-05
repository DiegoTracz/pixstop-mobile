package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallPaged
import com.pixstop.mobile.data.remote.dto.AllocateRequest
import com.pixstop.mobile.data.remote.dto.BalanceRequest
import com.pixstop.mobile.data.remote.dto.CompanyDashboardDto
import com.pixstop.mobile.data.remote.dto.CompanyOrderDto
import com.pixstop.mobile.data.remote.dto.CompanyUserDto
import com.pixstop.mobile.data.remote.dto.DepartmentDto
import com.pixstop.mobile.data.remote.dto.DistributeRequest
import com.pixstop.mobile.data.remote.dto.OrderActionRequest
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.data.remote.dto.ToggleRequest
import com.pixstop.mobile.domain.model.CompanyDashboard
import com.pixstop.mobile.domain.model.CompanyMember
import com.pixstop.mobile.domain.model.CompanyOrder
import com.pixstop.mobile.domain.model.CompanyRole
import com.pixstop.mobile.domain.model.Department
import com.pixstop.mobile.domain.model.LowStockProduct
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.TopProduct
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonElement

private const val TAG = "Company"

/**
 * Painel do administrador da empresa.
 *
 * Toda rota daqui exige o papel de admin no servidor; o app esconde a área,
 * mas quem decide é o middleware.
 */
class CompanyRepository(private val client: HttpClient) {

    suspend fun dashboard(): Outcome<CompanyDashboard> =
        safeCall<CompanyDashboardDto>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_DASHBOARD)
        }.map { it.toDomain() }

    suspend fun orders(page: Int = 1, status: String? = null): Outcome<Page<CompanyOrder>> =
        safeCallPaged<CompanyOrderDto>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_ORDERS) {
                parameter("page", page)
                status?.let { parameter("status", it) }
            }
        }.map { result -> Page(result.items.map { it.toDomain() }, result.meta) }

    suspend fun users(): Outcome<List<CompanyMember>> =
        safeCallPaged<CompanyUserDto>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_USERS) { parameter("per_page", 100) }
        }.map { result -> result.items.map { it.toDomain() } }

    suspend fun departments(): Outcome<List<Department>> =
        safeCall<List<DepartmentDto>>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_DEPARTMENTS)
        }.map { list -> list.map { it.toDomain() } }

    /**
     * Aprova à mão ou cancela. O motivo é obrigatório dos dois lados porque
     * quem comprou vai lê-lo.
     */
    suspend fun orderAction(orderId: Long, action: String, reason: String): Outcome<Unit> =
        safeCall<JsonElement>(TAG) {
            client.post(ApiConfig.Endpoints.companyOrderStatus(orderId)) {
                setBody(OrderActionRequest(action, reason))
            }
        }.map { }

    /**
     * Liga ou desliga o acesso de alguém.
     *
     * Ao desligar, os pixels voltam para o cofre da empresa por padrão: deixá-los
     * numa conta sem acesso os tornaria inalcançáveis.
     */
    suspend fun toggleUser(userId: Long, returnPixels: Boolean = true): Outcome<Boolean> =
        safeCall<ToggleResultDto>(TAG) {
            client.post(ApiConfig.Endpoints.companyUserToggle(userId)) { setBody(ToggleRequest(returnPixels)) }
        }.map { it.isActive }

    suspend fun adjustBalance(
        userIds: List<Long>,
        action: String,
        amount: Double,
        reason: String?,
    ): Outcome<Unit> =
        safeCall<JsonElement>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_BALANCE) {
                setBody(BalanceRequest(userIds, action, amount, reason?.takeIf { it.isNotBlank() }))
            }
        }.map { }

    /** Move pixels do cofre da empresa para a verba de um departamento. */
    suspend fun allocateToDepartment(departmentId: Long, amount: Int, reason: String?): Outcome<Unit> =
        safeCall<JsonElement>(TAG) {
            client.post(ApiConfig.Endpoints.companyDepartmentAllocate(departmentId)) {
                setBody(AllocateRequest(amount, reason?.takeIf { it.isNotBlank() }))
            }
        }.map { }

    /** Distribui pixels do cofre direto para pessoas, sem passar por time. */
    suspend fun distributePixels(userIds: List<Long>, amount: Int, reason: String?): Outcome<Unit> =
        safeCall<JsonElement>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_PIXELS_DISTRIBUTE) {
                setBody(DistributeRequest(userIds, amount, reason?.takeIf { it.isNotBlank() }))
            }
        }.map { }
}

@kotlinx.serialization.Serializable
private data class ToggleResultDto(
    @kotlinx.serialization.SerialName("is_active") val isActive: Boolean = true,
)

private fun CompanyDashboardDto.toDomain() = CompanyDashboard(
    periodDays = periodDays,
    orders = sales.orders,
    revenue = sales.revenue,
    fees = sales.fees,
    netRevenue = sales.netRevenue,
    profit = sales.profit,
    pixelsRedeemed = sales.pixels,
    topProducts = topProducts.map { TopProduct(it.product, it.quantity, it.revenue) },
    lowStock = lowStock.map { LowStockProduct(it.id, it.name, it.stock) },
    membersTotal = members.total,
    membersActive = members.active,
    corporatePixels = wallet.corporatePixels,
    departmentPixels = wallet.departmentPixels,
    companyBalance = wallet.companyBalance,
)

private fun CompanyOrderDto.toDomain() = CompanyOrder(
    id = id,
    transactionId = transactionId,
    buyerName = userName,
    status = OrderStatus.from(status),
    statusLabel = statusLabel ?: OrderStatus.from(status).name,
    paymentMethodLabel = paymentMethodLabel,
    itemsCount = itemsCount,
    totalMoney = totalMoney,
    totalBalance = totalBalance,
    totalPixels = totalPixels,
    netRevenue = netRevenue,
    createdAt = createdAt,
)

private fun CompanyUserDto.toDomain() = CompanyMember(
    id = id,
    name = name,
    email = email,
    role = CompanyRole.from(role),
    isActive = isActive,
    balance = balance,
    pixelAvailable = pixelAvailable,
)

private fun DepartmentDto.toDomain() = Department(
    id = id,
    name = name,
    description = description,
    pixelBalance = pixelBalance,
    membersCount = membersCount,
    isActive = isActive,
)
