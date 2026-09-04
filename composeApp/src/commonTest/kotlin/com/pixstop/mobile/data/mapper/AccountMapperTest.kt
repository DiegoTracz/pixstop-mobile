package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.remote.dto.MeDto
import com.pixstop.mobile.domain.model.CompanyRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O `/me` é a única fonte do estado da conta. Se a tradução dele errar, o app
 * inteiro mostra número trocado — daí a cobertura em cima do JSON real que o
 * backend devolve.
 */
class AccountMapperTest {

    private val respostaCompleta = """
    {
      "user": { "id": 7, "name": "Ana Souza", "email": "ana@exemplo.com", "is_admin_master": false },
      "tenants": [
        { "id": "abc", "name": "Padaria Alpha", "company_code": "ALPHA123", "role": "admin-company",
          "balance": 50.5, "pixel_balance": 3000, "pixel_available": 2500, "active": true, "is_active": true },
        { "id": "def", "name": "Mercado Beta", "role": "user",
          "balance": 0, "pixel_balance": 0, "pixel_available": 0, "active": false, "is_active": false }
      ],
      "active_tenant": {
        "id": "abc", "name": "Padaria Alpha", "company_code": "ALPHA123", "role": "admin-company",
        "is_manager": true, "balance": 50.5, "pixel_balance": 3000, "pixel_available": 2500,
        "department": { "id": 3, "name": "Vendas" },
        "managed_departments": [ { "id": 3, "name": "Vendas", "pixel_balance": 20000 } ],
        "subscription_status": "active", "plan_active": true,
        "mercadopago_connected": true, "cashback_enabled": true,
        "features": { "orders": true, "iot": false }
      },
      "has_pending_consent": false
    }
    """.trimIndent()

    @Test
    fun `traduz a conta inteira do JSON do backend`() {
        val account = apiJson.decodeFromString<MeDto>(respostaCompleta).toDomain()

        assertEquals("Ana Souza", account.user.name)
        assertEquals("Ana", account.user.firstName)
        assertEquals(2, account.memberships.size)
        assertFalse(account.hasPendingConsent)

        val company = account.activeCompany!!
        assertEquals("Padaria Alpha", company.name)
        assertEquals(CompanyRole.Admin, company.role)
        assertTrue(company.isManager)
        assertEquals(2500, company.pixelAvailable)
        assertEquals("Vendas", company.department?.name)
        assertEquals(20000, company.managedDepartments.single().pixelBalance)
    }

    @Test
    fun `funcionalidade ausente do plano conta como ligada`() {
        val company = apiJson.decodeFromString<MeDto>(respostaCompleta).toDomain().activeCompany!!

        assertTrue(company.hasFeature("orders"), "declarada como true")
        assertFalse(company.hasFeature("iot"), "declarada como false")
        // Um plano antigo não lista funcionalidades criadas depois dele; negar
        // por omissão tiraria da empresa algo que ela já usava.
        assertTrue(company.hasFeature("qualquer_coisa_nova"))
    }

    @Test
    fun `papel desconhecido cai no mais restrito`() {
        val json = """
        { "user": { "id": 1, "name": "X", "email": "x@x.com" },
          "tenants": [], "active_tenant": { "id": "a", "name": "A", "role": "papel-do-futuro" },
          "has_pending_consent": false }
        """.trimIndent()

        val company = apiJson.decodeFromString<MeDto>(json).toDomain().activeCompany!!

        // Uma versão antiga do app não pode abrir portas por não reconhecer um
        // papel que o servidor inventou depois.
        assertEquals(CompanyRole.Member, company.role)
    }

    @Test
    fun `sem empresa ativa, o app sabe que precisa de uma`() {
        val json = """
        { "user": { "id": 1, "name": "X", "email": "x@x.com" },
          "tenants": [], "active_tenant": null, "has_pending_consent": true }
        """.trimIndent()

        val account = apiJson.decodeFromString<MeDto>(json).toDomain()

        assertNull(account.activeCompany)
        assertTrue(account.needsCompany)
        assertFalse(account.canSwitchCompany)
        assertTrue(account.hasPendingConsent)
    }

    @Test
    fun `trocar de empresa so aparece com mais de um vinculo ativo`() {
        val account = apiJson.decodeFromString<MeDto>(respostaCompleta).toDomain()

        // Duas empresas na lista, mas uma com o vínculo desativado: não há
        // troca possível, e oferecer a opção seria mentira.
        assertFalse(account.canSwitchCompany)
        assertEquals(1, account.memberships.count { it.isActive })
    }

    @Test
    fun `sem gateway conectado, a empresa nao recebe dinheiro`() {
        val json = """
        { "user": { "id": 1, "name": "X", "email": "x@x.com" }, "tenants": [],
          "active_tenant": { "id": "a", "name": "A", "mercadopago_connected": false },
          "has_pending_consent": false }
        """.trimIndent()

        val company = apiJson.decodeFromString<MeDto>(json).toDomain().activeCompany!!

        assertFalse(company.canPayWithMoney)
    }
}
