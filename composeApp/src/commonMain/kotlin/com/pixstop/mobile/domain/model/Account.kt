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
    /**
     * Quem empreende com geladeiras (Fase 16): compra o estoque, coloca
     * geladeiras em várias empresas e repõe. Nulo para todo mundo que não é,
     * e é assim que o app decide se existe painel de operador.
     */
    val operator: OperatorAccount? = null,
) {
    /** Sem empresa escolhida, o app só oferece entrar em uma. */
    val needsCompany: Boolean get() = activeCompany == null

    /** Trocar de empresa só faz sentido com mais de uma. */
    val canSwitchCompany: Boolean get() = memberships.count { it.isActive } > 1

    /** O painel do operador só existe para quem faz parte de um. */
    val isOperator: Boolean get() = operator != null
}

/**
 * A conta de operador de quem está usando o app (Fase 16).
 *
 * `seesMoney` é do servidor, não do app: o repositor simplesmente não recebe
 * receita nem margem nas respostas, e esta bandeira existe para a tela não
 * prometer um número que nunca vai chegar.
 */
data class OperatorAccount(
    val id: Long,
    val name: String,
    val role: String?,
    val seesMoney: Boolean,
    /**
     * Esperando a liberação da plataforma (Fase 16, O8/P5).
     *
     * O painel abre assim mesmo, e vem vazio: quem espera ainda não tem
     * empresa nenhuma. A tela diz isso, em vez de deixar a pessoa achando
     * que o app quebrou.
     */
    val isPending: Boolean = false,
) {
    val isOwner: Boolean get() = role == "owner"
}

data class AccountUser(
    val id: Long,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    /** O código que a pessoa diz no balcão (Fase 14). */
    val memberCode: String? = null,
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
    /** Opera esta empresa (Fase 16), com ou sem papel de admin nela. */
    val operates: Boolean = false,
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
    /** Segmento (Fase 14): `pantry` é a geladeira, o que todo tenant era. */
    val vertical: String = "pantry",
    /** Os módulos do segmento; vazio quer dizer servidor antigo — tudo ligado. */
    val modules: List<String> = emptyList(),
    val memberNoun: String = "colaborador",
    /** `customer` quando a pessoa compra da empresa em vez de trabalhar nela. */
    val isCustomer: Boolean = false,
    /** Atende no balcão: registra visitas (staff ou administrador). */
    val isStaff: Boolean = false,
    /**
     * Opera esta empresa (Fase 16): pertence ao operador dela. Anda junto com
     * o papel, e não no lugar dele — quem cria a própria empresa administra e
     * opera ao mesmo tempo.
     */
    val operates: Boolean = false,
    /** Nulo quando a progressão por XP não está ligada nesta empresa. */
    val progression: Progression? = null,
    val pixelsExpiringSoon: Int = 0,
) {
    /**
     * Cuida da operação: geladeira, estoque e pedido.
     *
     * É o mesmo corte que o servidor faz entre `company.manager` e
     * `company.admin`; o app não pode abrir mais portas do que ele.
     */
    val canManageOperation: Boolean get() = role.isAdmin || operates

    /** Uma funcionalidade ausente do plano é considerada ligada. */
    fun hasFeature(name: String): Boolean = features[name] ?: true

    /** Sem lista de módulos (servidor antigo) tudo está ligado. */
    fun hasModule(name: String): Boolean = modules.isEmpty() || name in modules

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
    /** Quem opera a geladeira desta empresa sem administrá-la (Fase 16). */
    Operator("operator"),
    Member("user");

    val isAdmin: Boolean get() = this == Admin

    companion object {
        fun from(value: String?): CompanyRole =
            entries.firstOrNull { it.apiValue == value } ?: Member
    }
}
