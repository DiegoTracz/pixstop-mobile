package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.ApplianceChoice
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.Presence
import com.pixstop.mobile.support.FakeApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * De qual geladeira é a compra (Fase 9.5).
 *
 * O estoque, a reserva e a porta que abre são de uma geladeira só. Com uma
 * porta na empresa, nada muda e ninguém escolhe nada; com duas, a pessoa
 * precisa dizer em frente a qual está antes de comprar.
 */
class ApplianceChoiceTest {

    private val twoDoors = """
        {"success":true,"data":{
          "must_choose":true,
          "current_id":null,
          "appliances":[
            {"id":1,"name":"1º andar","location":"Copa","type":"refrigerator","presence":"online"},
            {"id":2,"name":"3º andar","location":null,"type":"refrigerator","presence":"offline"}
          ]}}
    """.trimIndent()

    private val singleDoor = """
        {"success":true,"data":{
          "must_choose":false,
          "current_id":7,
          "appliances":[
            {"id":7,"name":"Copa","location":null,"type":"refrigerator","presence":"online"}
          ]}}
    """.trimIndent()

    @Test
    fun `com duas portas o app precisa perguntar qual`() = runTest {
        val result = ShopRepository(FakeApi().clientReturning(twoDoors)).appliances()

        assertIs<Outcome.Success<ApplianceChoice>>(result)

        val choice = result.value

        assertTrue(choice.mustChoose)
        assertTrue(choice.hasChoice)
        assertNull(choice.current)
        assertEquals(2, choice.appliances.size)
    }

    @Test
    fun `com uma porta so nao ha pergunta a fazer`() = runTest {
        val result = ShopRepository(FakeApi().clientReturning(singleDoor)).appliances()

        assertIs<Outcome.Success<ApplianceChoice>>(result)

        val choice = result.value

        assertFalse(choice.mustChoose)
        assertFalse(choice.hasChoice)
        assertEquals("Copa", choice.current?.name)
    }

    @Test
    fun `a geladeira fora do ar continua na lista dizendo que esta fora`() = runTest {
        val result = ShopRepository(FakeApi().clientReturning(twoDoors)).appliances()

        assertIs<Outcome.Success<ApplianceChoice>>(result)

        // Some-la esconderia justamente a razão de a compra não sair ali.
        val terceiro = result.value.appliances.last()

        assertEquals("3º andar", terceiro.name)
        assertEquals(Presence.Offline, terceiro.presence)
    }

    @Test
    fun `o local aparece embaixo do nome quando existe`() = runTest {
        val result = ShopRepository(FakeApi().clientReturning(twoDoors)).appliances()

        assertIs<Outcome.Success<ApplianceChoice>>(result)

        assertEquals("Copa", result.value.appliances.first().subtitle)
        assertNull(result.value.appliances.last().subtitle)
    }
}
