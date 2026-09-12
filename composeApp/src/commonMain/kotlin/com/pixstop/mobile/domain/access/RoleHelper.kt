package com.pixstop.mobile.domain.access

import com.pixstop.mobile.domain.model.ActiveCompany

/**
 * Quem enxerga o quê.
 *
 * Três coisas decidem: o papel na empresa, gerir ou não um departamento, e as
 * funcionalidades que o plano contratado liga. Elas não formam uma escada —
 * alguém pode ser gestor de um time sem ser administrador da empresa, e o
 * administrador pode não gerir time nenhum.
 *
 * O servidor continua sendo quem decide de verdade; isto existe para o app não
 * oferecer o que ele vai recusar.
 */
object RoleHelper {

    /**
     * Os destinos da barra inferior.
     *
     * Sai desta lista, e não de uma lista fixa na tela, porque a regra é que
     * toda aba precisa levar a algum lugar para todo papel. Montada à mão, uma
     * aba sobreviveria a um plano que desligou a funcionalidade dela e levaria
     * a uma tela em branco.
     */
    fun bottomBar(company: ActiveCompany?): List<Destination> =
        listOf(Destination.Home, Destination.Shop, Destination.Profile)
            .filter { canOpen(it, company) }

    /**
     * O que entra no menu lateral, na ordem em que aparece.
     */
    fun drawer(company: ActiveCompany?, isOperator: Boolean = false): List<Destination> =
        listOf(
            Destination.Orders, Destination.Pixels, Destination.Progress, Destination.Rewards,
            Destination.Team, Destination.Staff, Destination.Company, Destination.Fridges,
            // O painel do operador não é da empresa ativa: é de quem faz parte
            // de um operador, e atravessa todas elas (Fase 16, O8).
            Destination.Operator,
        ).filter { canOpen(it, company, isOperator) }

    fun canOpen(destination: Destination, company: ActiveCompany?, isOperator: Boolean = false): Boolean {
        // O painel do operador é a exceção: não se responde a partir da
        // empresa ativa, porque quem opera atravessa várias e pode estar
        // olhando qualquer uma delas (Fase 16, O8).
        if (destination == Destination.Operator) {
            return isOperator
        }

        // Sem empresa ativa não há plano nem papel; a navegação já leva a
        // pessoa a entrar numa antes de chegar a qualquer uma destas telas.
        if (company == null) {
            return destination == Destination.Home || destination == Destination.Profile
        }

        if (destination.requiredFeature?.let { !company.hasFeature(it) } == true) {
            return false
        }

        if (destination.requiredModule?.let { !company.hasModule(it) } == true) {
            return false
        }

        return when (destination) {
            // Gerir um departamento não vem do papel: quem é `user` na empresa
            // pode gerir um time, e o administrador pode não gerir nenhum.
            Destination.Team -> company.isManager
            // O balcão é novo: só aparece quando o servidor diz que o segmento
            // o tem — num servidor antigo, sem lista de módulos, não existe.
            Destination.Staff -> "checkin" in company.modules && (company.isStaff || company.role.isAdmin)
            // Recompensas também são novas: só com o módulo declarado.
            Destination.Rewards -> "vouchers" in company.modules
            Destination.Company -> company.role.isAdmin
            // Geladeira é operação, e quem opera cuida dela sem administrar a
            // empresa (Fase 16, O8) — o mesmo corte que o servidor faz.
            Destination.Fridges -> company.canManageOperation
            else -> true
        }
    }
}
