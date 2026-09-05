package com.pixstop.mobile.data.repository

import com.pixstop.mobile.data.remote.dto.UpdatedProfileDto
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.support.FakeApi
import com.pixstop.mobile.ui.viewmodel.ProfileUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * O servidor espera `current_password` e `password_confirmation` em snake_case.
 * Um nome errado no corpo vira um 422 genérico, que na tela pareceria "senha
 * atual incorreta" — daí conferir o que sai, e não só o que volta.
 */
class ProfileRepositoryTest {

    @Test
    fun `troca de senha envia os campos que o servidor espera`() = runTest {
        val api = FakeApi()

        val result = ProfileRepository(api.clientReturning("""{"success":true,"data":null}"""))
            .updatePassword("antiga", "novaSenha1", "novaSenha1")

        assertIs<Outcome.Success<Unit>>(result)
        assertTrue(api.lastBody.contains("\"current_password\""), api.lastBody)
        assertTrue(api.lastBody.contains("\"password_confirmation\""), api.lastBody)
    }

    @Test
    fun `perfil envia nome e email sem espacos nas pontas`() = runTest {
        val api = FakeApi()

        val result = ProfileRepository(api.clientReturning("""{"success":true,"data":{"id":2,"name":"Carlos Silva","email":"carlos@alpha.com"}}"""))
            .update("  Carlos Silva  ", " carlos@alpha.com ")

        assertIs<Outcome.Success<*>>(result)
        assertTrue(api.lastBody.contains("\"name\":\"Carlos Silva\""), api.lastBody)
        assertTrue(api.lastBody.contains("\"email\":\"carlos@alpha.com\""), api.lastBody)
    }

    @Test
    fun `perfil devolve os dados como o servidor gravou`() = runTest {
        val result = ProfileRepository(
            FakeApi().clientReturning("""{"success":true,"data":{"id":2,"name":"Carlos S.","email":"novo@alpha.com"}}"""),
        ).update("Carlos S.", "novo@alpha.com")

        assertIs<Outcome.Success<UpdatedProfileDto>>(result)
        assertEquals("novo@alpha.com", result.value.email)
        assertEquals("Carlos S.", result.value.name)
    }

    @Test
    fun `senha curta ou diferente da confirmacao nao libera o botao`() {
        val curta = ProfileUiState(currentPassword = "antiga", newPassword = "1234", confirmation = "1234")
        assertFalse(curta.canSavePassword)

        val diferente = ProfileUiState(currentPassword = "antiga", newPassword = "novaSenha1", confirmation = "outraSenha1")
        assertFalse(diferente.canSavePassword)
        assertFalse(diferente.passwordsMatch)

        val ok = ProfileUiState(currentPassword = "antiga", newPassword = "novaSenha1", confirmation = "novaSenha1")
        assertTrue(ok.canSavePassword)
    }

    @Test
    fun `perfil sem nome ou email nao libera o botao`() {
        assertFalse(ProfileUiState(name = "", email = "a@b.com").canSaveProfile)
        assertFalse(ProfileUiState(name = "Carlos", email = "   ").canSaveProfile)
        assertTrue(ProfileUiState(name = "Carlos", email = "a@b.com").canSaveProfile)
    }
}
