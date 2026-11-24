package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.ChatRole
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.support.FakeApi
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O contrato do chat do assistente (ASSISTENTE_IA.md, A8): as respostas copiam
 * o envelope que o servidor devolve pela API do app.
 */
class AssistantRepositoryTest {

    private val turnoJson = """
        {"success": true, "data": {
          "conversation": {"id": 7, "title": "Cadastre este produto."},
          "messages": [
            {"id": 30, "role": "user", "content": "Cadastre este produto.", "attachments": [{"id": 4, "mime": "image/jpeg", "url": "https://x.test/api/company/assistant/attachments/4"}]},
            {"id": 31, "role": "tool", "tool_name": "create_product_from_photo", "content": "Pela foto: Coca-Cola 200ml",
             "result": {"success": true, "message": "Pela foto: Coca-Cola 200ml", "preview": false, "requires_user_confirmation": false,
                        "data": {"name": "Coca-Cola 200ml", "estimated_price": 4.0, "missing": ["price", "cost"]}}},
            {"id": 32, "role": "assistant", "content": "Qual o custo?"}
          ]
        }}
    """.trimIndent()

    @Test
    fun `a mensagem vai com a conversa e as fotos e volta o turno`() = runTest {
        val api = FakeApi()
        val outcome = AssistantRepository(api.clientReturning(turnoJson)).send(7, "Cadastre este produto.", listOf(4))

        val thread = (outcome as Outcome.Success).value
        assertEquals(7L, thread.conversation?.id)
        assertEquals(listOf(ChatRole.User, ChatRole.Tool, ChatRole.Assistant), thread.messages.map { it.role })
        assertEquals(1, thread.messages.first().photos)
        assertEquals("R$ 4,00", thread.messages[1].card?.fields?.toMap()?.get("Preço de referência"))

        assertEquals(HttpMethod.Post, api.lastRequest?.method)
        assertTrue(api.lastRequest!!.url.encodedPath.endsWith("company/assistant/messages"))
        assertTrue(api.lastBody.contains("\"conversation_id\":7"))
        assertTrue(api.lastBody.contains("\"attachment_ids\":[4]"))
    }

    @Test
    fun `a foto do produto sobe em multipart e volta o id`() = runTest {
        val api = FakeApi(HttpStatusCode.Created)
        val outcome = AssistantRepository(api.clientReturning("""{"success": true, "data": {"attachment": {"id": 9, "mime": "image/jpeg", "url": "u"}}}"""))
            .uploadPhoto(byteArrayOf(1, 2, 3))

        assertEquals(9L, (outcome as Outcome.Success).value.id)
        assertTrue(api.lastRequest!!.url.encodedPath.endsWith("company/assistant/attachments"))
        assertTrue(api.lastRequest!!.body.contentType.toString().startsWith("multipart/form-data"))
    }

    @Test
    fun `o botao do card confirma pelo decide`() = runTest {
        val api = FakeApi()
        AssistantRepository(api.clientReturning("""{"success": true, "data": {"messages": []}}"""))
            .decide(7, "create_product", "tok123", confirm = true)

        assertTrue(api.lastRequest!!.url.encodedPath.endsWith("company/assistant/conversations/7/decide"))
        assertTrue(api.lastBody.contains("\"decision\":\"confirm\""))
        assertTrue(api.lastBody.contains("\"confirmation_token\":\"tok123\""))
    }
}
