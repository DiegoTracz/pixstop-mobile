package com.pixstop.mobile.domain.access

import com.pixstop.mobile.domain.model.ActiveCompany
import com.pixstop.mobile.domain.model.CompanyRole
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Toda aba da barra inferior precisa levar a algum lugar, para todo papel — a
 * armadilha que o immo corrigiu na 1.2.0 foi exatamente uma aba que existia
 * para um papel e abria em branco para outro.
 *
 * Aqui a barra sai da matriz, então o buraco só apareceria se a própria matriz
 * dissesse que um destino visível não pode ser aberto. É isso que estes testes
 * vigiam.
 */
class RoleHelperTest {

    private fun empresa(
        role: CompanyRole = CompanyRole.Member,
        isManager: Boolean = false,
        features: Map<String, Boolean> = emptyMap(),
        modules: List<String> = emptyList(),
        isStaff: Boolean = false,
        operates: Boolean = false,
    ) = ActiveCompany(
        id = "1",
        name = "Empresa Alpha",
        companyCode = null,
        logoUrl = null,
        role = role,
        isManager = isManager,
        pixelBalance = 0,
        pixelAvailable = 0,
        department = null,
        managedDepartments = emptyList(),
        subscriptionStatus = "active",
        planActive = true,
        mercadoPagoConnected = false,
        cashbackEnabled = true,
        features = features,
        modules = modules,
        isStaff = isStaff,
        operates = operates,
    )

    private val papeis = listOf(
        empresa(CompanyRole.Member),
        empresa(CompanyRole.Manager, isManager = true),
        empresa(CompanyRole.Admin),
        empresa(CompanyRole.Admin, isManager = true),
        empresa(CompanyRole.Operator, operates = true),
        empresa(CompanyRole.Admin, operates = true),
    )

    @Test
    fun `nenhuma aba leva a lugar nenhum, em papel nenhum`() {
        papeis.forEach { company ->
            RoleHelper.bottomBar(company).forEach { destination ->
                assertTrue(
                    RoleHelper.canOpen(destination, company),
                    "a aba $destination aparece para ${company.role} mas não abre",
                )
            }
        }
    }

    @Test
    fun `o menu lateral so oferece o que o papel abre`() {
        papeis.forEach { company ->
            listOf(false, true).forEach { isOperator ->
                RoleHelper.drawer(company, isOperator).forEach { destination ->
                    assertTrue(
                        RoleHelper.canOpen(destination, company, isOperator),
                        "o menu oferece $destination para ${company.role} sem poder abrir",
                    )
                }
            }
        }
    }

    @Test
    fun `a area do gestor depende de gerir um time, nao do papel`() {
        // Quem é `user` na empresa pode gerir um departamento — é o caso do
        // seed —, e o administrador pode não gerir nenhum.
        assertTrue(RoleHelper.canOpen(Destination.Team, empresa(CompanyRole.Member, isManager = true)))
        assertFalse(RoleHelper.canOpen(Destination.Team, empresa(CompanyRole.Admin, isManager = false)))
    }

    @Test
    fun `o painel da empresa e so do administrador`() {
        assertTrue(RoleHelper.canOpen(Destination.Company, empresa(CompanyRole.Admin)))
        assertFalse(RoleHelper.canOpen(Destination.Company, empresa(CompanyRole.Manager, isManager = true)))
        assertFalse(RoleHelper.canOpen(Destination.Company, empresa(CompanyRole.Member)))
    }

    @Test
    fun `funcionalidade desligada no plano some da barra e do menu`() {
        val semLoja = empresa(CompanyRole.Admin, features = mapOf("products" to false))

        assertFalse(RoleHelper.bottomBar(semLoja).contains(Destination.Shop))
        assertFalse(RoleHelper.canOpen(Destination.Cart, semLoja))

        val semPixels = empresa(CompanyRole.Admin, features = mapOf("pixels" to false))

        assertFalse(RoleHelper.drawer(semPixels).contains(Destination.Pixels))
        assertContains(RoleHelper.drawer(semPixels), Destination.Orders)
    }

    @Test
    fun `funcionalidade que o servidor nao menciona conta como ligada`() {
        // Um plano antigo pode não citar uma funcionalidade nova. Esconder a
        // tela nesse caso puniria quem já tinha direito a ela.
        val company = empresa(CompanyRole.Admin, isManager = true, features = emptyMap())

        assertEquals(
            listOf(Destination.Orders, Destination.Pixels, Destination.Progress, Destination.Team, Destination.Company, Destination.Fridges),
            RoleHelper.drawer(company),
        )
    }

    @Test
    fun `quem opera a empresa cuida das geladeiras sem administrar a empresa`() {
        // O mesmo corte do servidor entre `company.manager` e `company.admin`
        // (Fase 16, O8): geladeira é operação, pessoas e plano não.
        val operada = empresa(CompanyRole.Operator, operates = true)

        assertTrue(RoleHelper.canOpen(Destination.Fridges, operada))
        assertFalse(RoleHelper.canOpen(Destination.Company, operada))

        // E quem só participa continua de fora.
        assertFalse(RoleHelper.canOpen(Destination.Fridges, empresa(CompanyRole.Member)))
    }

    @Test
    fun `quem cria a propria empresa administra e opera ao mesmo tempo`() {
        val minha = empresa(CompanyRole.Admin, operates = true)

        assertTrue(RoleHelper.canOpen(Destination.Fridges, minha))
        assertTrue(RoleHelper.canOpen(Destination.Company, minha))
    }

    @Test
    fun `o painel do operador so aparece para quem faz parte de um`() {
        val company = empresa(CompanyRole.Member)

        assertFalse(RoleHelper.drawer(company).contains(Destination.Operator))
        assertContains(RoleHelper.drawer(company, isOperator = true), Destination.Operator)

        // Ele atravessa empresas: existe mesmo sem uma escolhida.
        assertTrue(RoleHelper.canOpen(Destination.Operator, null, isOperator = true))
    }

    @Test
    fun `um papel que o app nao conhece cai no mais restrito`() {
        // Se o servidor inventar um papel novo, uma versão antiga do app não
        // pode abrir portas demais por não reconhecê-lo.
        assertEquals(CompanyRole.Member, CompanyRole.from("papel-que-nao-existe"))
        assertEquals(CompanyRole.Operator, CompanyRole.from("operator"))
    }

    @Test
    fun `sem empresa ativa so restam inicio e perfil`() {
        assertEquals(listOf(Destination.Home, Destination.Profile), RoleHelper.bottomBar(null))
        assertTrue(RoleHelper.drawer(null).isEmpty())
    }

    @Test
    fun `inicio e perfil existem para todo papel`() {
        (papeis + null).forEach { company ->
            assertTrue(RoleHelper.canOpen(Destination.Home, company))
            assertTrue(RoleHelper.canOpen(Destination.Profile, company))
        }
    }

    @Test
    fun `o segmento sem loja esconde loja, carrinho e pedidos, e sem departamentos esconde o time`() {
        val barbearia = empresa(role = CompanyRole.Admin, isManager = true, modules = listOf("checkin", "vouchers"))

        assertFalse(RoleHelper.bottomBar(barbearia).contains(Destination.Shop))
        assertFalse(RoleHelper.canOpen(Destination.Cart, barbearia))
        assertFalse(RoleHelper.drawer(barbearia).contains(Destination.Orders))
        assertFalse(RoleHelper.drawer(barbearia).contains(Destination.Team))
        assertTrue(RoleHelper.drawer(barbearia).contains(Destination.Pixels))
        assertTrue(RoleHelper.drawer(barbearia).contains(Destination.Company))
    }

    @Test
    fun `sem lista de modulos o servidor antigo continua com tudo ligado`() {
        val geladeira = empresa(isManager = true)

        assertTrue(RoleHelper.bottomBar(geladeira).contains(Destination.Shop))
        assertTrue(RoleHelper.drawer(geladeira).contains(Destination.Team))
    }

    @Test
    fun `o balcao aparece para o staff e para o administrador, onde ha visita registrada`() {
        val barbearia = listOf("checkin", "vouchers")

        assertTrue(RoleHelper.drawer(empresa(isStaff = true, modules = barbearia)).contains(Destination.Staff))
        assertTrue(RoleHelper.drawer(empresa(role = CompanyRole.Admin, modules = barbearia)).contains(Destination.Staff))
        assertFalse(RoleHelper.drawer(empresa(modules = barbearia)).contains(Destination.Staff))
        // A geladeira não tem balcão, por mais staff que a pessoa seja.
        assertFalse(RoleHelper.canOpen(Destination.Staff, empresa(isStaff = true, modules = listOf("departments", "shop"))))
    }

    @Test
    fun `recompensas aparecem so onde o segmento tem vouchers`() {
        assertTrue(RoleHelper.drawer(empresa(modules = listOf("checkin", "vouchers"))).contains(Destination.Rewards))
        assertFalse(RoleHelper.drawer(empresa(modules = listOf("departments", "shop"))).contains(Destination.Rewards))
        assertFalse(RoleHelper.drawer(empresa()).contains(Destination.Rewards))
    }
}
