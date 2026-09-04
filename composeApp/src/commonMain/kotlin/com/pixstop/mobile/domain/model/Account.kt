package com.pixstop.mobile.domain.model

/**
 * Quem está usando o app e em que empresa.
 *
 * É o estado que quase toda tela consulta, então mora em um lugar só e é
 * atualizado por inteiro a cada `/me` — nunca em pedaços.
 */
data class Account(
    val user: AccountUser,
    val memberships: List<Membership>,
    val activeCompany: ActiveCompany?,
    val hasPendingConsent: Boolean,
) {
    /** Sem empresa escolhida, o app só oferece entrar em uma. */
    val needsCompany: Boolean get() = activeCompany == null

    /** Trocar de empresa só faz sentido com mais de uma. */
    val canSwitchCompany: Boolean get() = memberships.count { it.isActive } > 1
}

data class AccountUser(
    val id: Long,
    val name: String,
    val email: String,
    val avatarUrl: String?,
) {
    /** Primeiro nome, que é como o app se dirige à pessoa. */
    val firstName: String get() = name.trim().substringBefore(' ')
}

data class Membership(
    val id: String,
    val name: String,
    val companyCode: String?,
    val role: CompanyRole,
    val balance: Double,
    val pixelAvailable: Int,
    val isCurrent: Boolean,
    val isActive: Boolean,
)

data class ActiveCompany(
    val id: String,
    val name: String,
    val companyCode: String?,
    val logoUrl: String?,
    val role: CompanyRole,
    val isManager: Boolean,
    val balance: Double,
    val pixelBalance: Int,
    val pixelAvailable: Int,
    val department: NamedRef?,
    val managedDepartments: List<ManagedDepartment>,
    val subscriptionStatus: String?,
    val planActive: Boolean,
    val mercadoPagoConnected: Boolean,
    val cashbackEnabled: Boolean,
    val features: Map<String, Boolean>,
) {
    /** Uma funcionalidade ausente do plano é considerada ligada. */
    fun hasFeature(name: String): Boolean = features[name] ?: true

    /** Sem gateway conectado, só saldo e pixels pagam. */
    val canPayWithMoney: Boolean get() = mercadoPagoConnected
}

data class NamedRef(val id: Long, val name: String)

data class ManagedDepartment(val id: Long, val name: String, val pixelBalance: Int)

/**
 * Papel dentro da empresa. Define o que aparece no menu.
 *
 * Um papel desconhecido cai em `Member`, o mais restrito: se o servidor
 * inventar um papel novo, uma versão antiga do app não pode abrir portas
 * demais por não reconhecê-lo.
 */
enum class CompanyRole(val apiValue: String) {
    Admin("admin-company"),
    Manager("manager"),
    Member("user");

    val isAdmin: Boolean get() = this == Admin

    companion object {
        fun from(value: String?): CompanyRole =
            entries.firstOrNull { it.apiValue == value } ?: Member
    }
}
