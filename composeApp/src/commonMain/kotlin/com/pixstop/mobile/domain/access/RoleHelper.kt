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
    fun drawer(company: ActiveCompany?): List<Destination> =
        listOf(Destination.Orders, Destination.Pixels, Destination.Progress, Destination.Rewards, Destination.Team, Destination.Staff, Destination.Company, Destination.Fridges)
            .filter { canOpen(it, company) }

    fun canOpen(destination: Destination, company: ActiveCompany?): Boolean {
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
            Destination.Fridges -> company.role.isAdmin
            else -> true
        }
    }
}
