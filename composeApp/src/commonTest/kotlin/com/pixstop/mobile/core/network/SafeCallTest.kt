package com.pixstop.mobile.core.network

import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@Serializable
private data class Sample(val id: Int, val name: String)

/**
 * O `safeCall` é o único lugar que traduz resposta HTTP em erro de domínio.
 * Se ele errar, toda tela erra junto — daí a cobertura de cada caso que o
 * backend pode devolver.
 */
class SafeCallTest {

    private fun clientReturning(status: HttpStatusCode, body: String) = HttpClient(
        MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
    )

    @Test
    fun `entrega os dados quando a resposta vem no envelope`() = runTest {
        val client = clientReturning(
            HttpStatusCode.OK,
            """{"success":true,"data":{"id":7,"name":"Coca-Cola 2L"}}""",
        )

        val result = safeCall<Sample>("Teste") { client.get("qualquer") }

        assertIs<Outcome.Success<Sample>>(result)
        assertEquals(7, result.value.id)
        assertEquals("Coca-Cola 2L", result.value.name)
    }

    @Test
    fun `lista paginada traz os itens e o rodape`() = runTest {
        val client = clientReturning(
            HttpStatusCode.OK,
            """{"success":true,"data":[{"id":1,"name":"A"},{"id":2,"name":"B"}],
                "meta":{"current_page":1,"last_page":3,"per_page":2,"total":6}}""",
        )

        val result = safeCallPaged<Sample>("Teste") { client.get("qualquer") }

        assertIs<Outcome.Success<Page<Sample>>>(result)
        val page = result.value
        assertEquals(2, page.items.size)
        assertEquals(6, page.meta.total)
        assertTrue(page.meta.hasNextPage, "com 3 páginas, a primeira precisa indicar que há próxima")
    }

    @Test
    fun `codigo do backend vira o erro de dominio correspondente`() = runTest {
        val casos = listOf(
            Triple(HttpStatusCode.Conflict, "no_active_tenant", DomainError.NoActiveCompany::class),
            Triple(HttpStatusCode.Forbidden, "user_inactive", DomainError.UserInactive::class),
            Triple(HttpStatusCode.Forbidden, "plan_inactive", DomainError.PlanInactive::class),
            Triple(HttpStatusCode.Forbidden, "consent_required", DomainError.ConsentRequired::class),
        )

        casos.forEach { (status, code, esperado) ->
            val client = clientReturning(
                status,
                """{"success":false,"error":{"code":"$code","message":"mensagem do servidor"}}""",
            )

            val result = safeCall<Sample>("Teste") { client.get("qualquer") }

            assertIs<Outcome.Failure>(result)
            assertEquals(
                esperado,
                result.error::class,
                "o código $code precisa virar ${esperado.simpleName}",
            )
            assertEquals("mensagem do servidor", result.error.message)
        }
    }

    @Test
    fun `regra de negocio preserva o codigo para a tela decidir`() = runTest {
        val client = clientReturning(
            HttpStatusCode.UnprocessableEntity,
            """{"success":false,"error":{"code":"out_of_stock","message":"Sem estoque suficiente."}}""",
        )

        val result = safeCall<Sample>("Teste") { client.get("qualquer") }

        assertIs<Outcome.Failure>(result)
        val error = result.error
        assertIs<DomainError.Rule>(error)
        assertEquals("out_of_stock", error.code)
    }

    @Test
    fun `erro de validacao traz a mensagem de cada campo`() = runTest {
        val client = clientReturning(
            HttpStatusCode.UnprocessableEntity,
            """{"message":"Dados inválidos","errors":{"email":["Este e-mail já está em uso."]}}""",
        )

        val result = safeCall<Sample>("Teste") { client.get("qualquer") }

        assertIs<Outcome.Failure>(result)
        val error = result.error
        assertIs<DomainError.Validation>(error)
        assertEquals("Este e-mail já está em uso.", error.fieldErrors["email"])
        // A mensagem geral é a do primeiro campo: é o que a tela mostra no topo.
        assertEquals("Este e-mail já está em uso.", error.message)
    }

    @Test
    fun `token recusado vira sessao expirada`() = runTest {
        val client = clientReturning(HttpStatusCode.Unauthorized, """{"message":"Unauthenticated."}""")

        val result = safeCall<Sample>("Teste") { client.get("qualquer") }

        assertIs<Outcome.Failure>(result)
        assertIs<DomainError.Unauthorized>(result.error)
    }

    @Test
    fun `queda de rede vira offline, e nao erro de servidor`() = runTest {
        // A tela trata os dois de formas diferentes: um convida a tentar de
        // novo, o outro não.
        val client = HttpClient(MockEngine { throw kotlinx.io.IOException("Unable to resolve host") })

        val result = safeCall<Sample>("Teste") { client.get("qualquer") }

        assertIs<Outcome.Failure>(result)
        assertIs<DomainError.Offline>(result.error)
    }
}
