package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.model.AuthResponseData
import com.pixstop.mobile.data.model.ProfileData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

/**
 * O `/me` não devolve token — só login e registro devolvem.
 *
 * Decodificá-lo como resposta de autenticação fazia toda busca de perfil
 * falhar em silêncio e o app cair no cache, exibindo "Offline" logo depois
 * de um login bem-sucedido.
 */
class ProfileDecodingTest {

    /** Recorte fiel de `GET /api/me` do backend. */
    private val meResponse = """
        {
          "user": {
            "id": 2,
            "name": "Carlos Silva",
            "email": "carlos.silva@alpha.com",
            "avatar_url": "http://127.0.0.1:8010/images/avatar/profile_image.png",
            "is_admin_master": false
          },
          "tenants": [
            {"id":"822d2ed6","name":"Empresa Alpha","company_code":"VCI4THG6","role":"admin-company","balance":0,"active":false,"is_active":true},
            {"id":"850b6a0c","name":"Empresa Beta","company_code":"Z6DZ1JB3","role":"user","balance":75,"active":true,"is_active":true}
          ],
          "active_tenant": {
            "id": "850b6a0c",
            "name": "Empresa Beta",
            "company_code": "Z6DZ1JB3",
            "role": "user",
            "balance": 75,
            "is_active": true
          }
        }
    """.trimIndent()

    @Test
    fun `perfil decodifica a resposta do me`() {
        val profile = apiJson.decodeFromString<ProfileData>(meResponse)

        assertEquals("Carlos Silva", profile.user.name)
        assertEquals(2, profile.tenants?.size)
        assertEquals("Empresa Beta", profile.activeTenant?.name)
    }

    @Test
    fun `resposta do me nao serve como resposta de autenticacao`() {
        assertFails { apiJson.decodeFromString<AuthResponseData>(meResponse) }
    }
}
