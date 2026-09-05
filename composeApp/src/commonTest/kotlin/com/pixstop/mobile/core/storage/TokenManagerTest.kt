package com.pixstop.mobile.core.storage

import com.pixstop.mobile.support.InMemorySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O token é credencial e vai no armazenamento cifrado. Duas coisas podem dar
 * errado nessa mudança, e as duas são silenciosas: deslogar quem já estava
 * dentro, e deixar a credencial antiga esquecida no lugar velho.
 */
class TokenManagerTest {

    private val common = InMemorySettings()
    private val secure = InMemorySettings()
    private val tokens = TokenManager(settings = common, secure = secure)

    @Test
    fun `o token novo nasce no armazenamento cifrado`() {
        tokens.saveToken("abc123")

        assertEquals("abc123", secure.getStringOrNull("auth_token"))
        assertNull(common.getStringOrNull("auth_token"), "não pode sobrar cópia no comum")
    }

    @Test
    fun `quem ja estava logado tem o token migrado na primeira leitura`() {
        // Estado de quem atualizou o aplicativo: o token ficou onde estava.
        common.putString("auth_token", "token-antigo")

        assertEquals("token-antigo", tokens.getToken())
        assertEquals("token-antigo", secure.getStringOrNull("auth_token"))
        assertNull(common.getStringOrNull("auth_token"), "o lugar antigo tem de ficar vazio")
    }

    @Test
    fun `o cifrado tem precedencia sobre um resto do armazenamento antigo`() {
        secure.putString("auth_token", "token-novo")
        common.putString("auth_token", "token-antigo")

        assertEquals("token-novo", tokens.getToken())
    }

    @Test
    fun `sair da conta nao deixa credencial esquecida em lugar nenhum`() {
        secure.putString("auth_token", "token-novo")
        common.putString("auth_token", "token-antigo")

        tokens.clearToken()

        assertNull(secure.getStringOrNull("auth_token"))
        assertNull(common.getStringOrNull("auth_token"))
        assertFalse(tokens.hasToken())
    }

    @Test
    fun `sem token em lugar nenhum a sessao nao existe`() {
        assertNull(tokens.getToken())
        assertFalse(tokens.hasToken())
    }

    @Test
    fun `o cache do perfil continua no armazenamento comum`() {
        tokens.saveUserData(
            com.pixstop.mobile.data.model.CachedUserData(
                user = com.pixstop.mobile.data.model.User(id = 2, name = "Carlos", email = "c@alpha.com"),
            ),
        )

        assertTrue(common.keys.contains("cached_user_data"), "o cache não é credencial")
        assertFalse(secure.keys.contains("cached_user_data"))
    }
}
