package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.data.remote.dto.ActiveTenantDto
import com.pixstop.mobile.data.remote.dto.MeDto
import com.pixstop.mobile.data.remote.dto.MembershipDto
import com.pixstop.mobile.domain.model.Account
import com.pixstop.mobile.domain.model.AccountUser
import com.pixstop.mobile.domain.model.ActiveCompany
import com.pixstop.mobile.domain.model.CompanyRole
import com.pixstop.mobile.domain.model.ManagedDepartment
import com.pixstop.mobile.domain.model.Membership
import com.pixstop.mobile.domain.model.NamedRef

/**
 * Traduz o que a API devolve para o que o app entende.
 *
 * A fronteira existe para que um campo novo ou renomeado no servidor mexa em um
 * arquivo só, e não em toda tela que exibe saldo.
 */
fun MeDto.toDomain(): Account = Account(
    user = AccountUser(
        id = user.id,
        name = user.name,
        email = user.email,
        avatarUrl = user.avatarUrl,
    ),
    memberships = tenants.map { it.toDomain() },
    activeCompany = activeTenant?.toDomain(),
    hasPendingConsent = hasPendingConsent,
)

fun MembershipDto.toDomain(): Membership = Membership(
    id = id,
    name = name,
    companyCode = companyCode,
    role = CompanyRole.from(role),
    balance = balance,
    pixelAvailable = pixelAvailable,
    isCurrent = active,
    isActive = isActive,
)

fun ActiveTenantDto.toDomain(): ActiveCompany = ActiveCompany(
    id = id,
    name = name,
    companyCode = companyCode,
    logoUrl = logoUrl,
    role = CompanyRole.from(role),
    isManager = isManager,
    balance = balance,
    pixelBalance = pixelBalance,
    pixelAvailable = pixelAvailable,
    department = department?.let { NamedRef(it.id, it.name) },
    managedDepartments = managedDepartments.map { ManagedDepartment(it.id, it.name, it.pixelBalance) },
    subscriptionStatus = subscriptionStatus,
    planActive = planActive,
    mercadoPagoConnected = mercadoPagoConnected,
    cashbackEnabled = cashbackEnabled,
    features = features,
)
