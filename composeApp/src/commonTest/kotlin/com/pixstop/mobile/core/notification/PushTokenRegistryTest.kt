package com.pixstop.mobile.core.notification

import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.core.storage.TokenManager
import com.pixstop.mobile.data.repository.NotificationRepository
import com.pixstop.mobile.support.FakeApi
import com.pixstop.mobile.support.InMemorySettings
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O token de push só vale no servidor com alguém logado, e precisa sair de lá
 * antes do logout — senão quem deixou o aparelho continua recebendo os avisos.
 */
class PushTokenRegistryTest {

    private val api = FakeApi(HttpStatusCode.Created)
    private val tokens = TokenManager(settings = InMemorySettings(), secure = InMemorySettings())
    private val registry = PushTokenRegistry(
        notifications = NotificationRepository(api.clientReturning("""{"success": true, "data": {"id": 1}}""")),
        session = SessionStore(tokens),
        appVersion = "1.5.0",
    )

    @Test
    fun `sem sessao o token espera e nao vai ao servidor`() = runTest {
        registry.onNewToken("fcm-123")

        assertNull(api.lastRequest)
    }

    @Test
    fun `com sessao o token vai ao servidor com a plataforma e a versao`() = runTest {
        registry.onNewToken("fcm-123")
        tokens.saveToken("sanctum")
        registry.sync()

        assertEquals(HttpMethod.Post, api.lastRequest?.method)
        assertTrue(api.lastRequest!!.url.encodedPath.endsWith("/devices"))
        assertTrue(api.lastBody.contains("\"token\":\"fcm-123\""))
        assertTrue(api.lastBody.contains("\"platform\":\"$devicePlatform\""))
        assertTrue(api.lastBody.contains("\"app_version\":\"1.5.0\""))
    }

    @Test
    fun `o mesmo token nao e enviado duas vezes na mesma execucao`() = runTest {
        tokens.saveToken("sanctum")
        registry.onNewToken("fcm-123")
        api.forget()

        registry.sync()

        assertNull(api.lastRequest)
    }

    @Test
    fun `token novo do sistema vai ao servidor de novo`() = runTest {
        tokens.saveToken("sanctum")
        registry.onNewToken("fcm-123")
        registry.onNewToken("fcm-456")

        assertTrue(api.lastBody.contains("\"token\":\"fcm-456\""))
    }

    @Test
    fun `falha no envio deixa o token para a proxima tentativa`() = runTest {
        tokens.saveToken("sanctum")
        api.status = HttpStatusCode.InternalServerError
        registry.onNewToken("fcm-123")

        api.status = HttpStatusCode.Created
        api.forget()
        registry.sync()

        assertTrue(api.lastBody.contains("\"token\":\"fcm-123\""))
    }

    @Test
    fun `sair apaga o aparelho no servidor e o proximo login registra de novo`() = runTest {
        tokens.saveToken("sanctum")
        registry.onNewToken("fcm-123")

        registry.unregister()

        assertEquals(HttpMethod.Delete, api.lastRequest?.method)
        assertTrue(api.lastRequest!!.url.encodedPath.endsWith("/devices/fcm-123"))

        api.forget()
        registry.sync()

        assertEquals(HttpMethod.Post, api.lastRequest?.method)
    }
}
