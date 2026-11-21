package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PrizeSpinKind
import com.pixstop.mobile.support.FakeApi
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O contrato da roleta de prêmios (docs/plans/LANCAMENTO_LOCARMAIS.md no servidor). */
class PrizeWheelRepositoryTest {

    private val wheelJson = """
        {"id": 3, "name": "Roleta de lançamento", "message": "Obrigado!", "ends_at": "2026-12-31T23:59:59-03:00",
         "available": {"gold": 12, "purchase": 1}, "guaranteed_jackpot": true, "won_pixels": 150, "purchase_min_total": 5,
         "slices": {
           "gold": [{"label": "50 moedas", "pixels": 50, "is_jackpot": false},
                    {"label": "Um energético por nossa conta", "pixels": 1200, "is_jackpot": true}],
           "purchase": [{"label": "10 moedas", "pixels": 10, "is_jackpot": false}]
         },
         "recent": [{"kind": "gold", "label": "100 moedas", "pixels": 100, "is_jackpot": false, "spun_at": "2026-12-01T10:00:00-03:00"}]}
    """.trimIndent()

    @Test
    fun `a roleta vem com os giros e as duas rodas`() = runTest {
        val api = FakeApi().clientReturning("""{"success": true, "data": {"wheel": $wheelJson}}""")
        val wheel = (PrizeWheelRepository(api).current() as Outcome.Success).value!!

        assertEquals(12, wheel.goldSpins)
        assertEquals(13, wheel.spinsLeft)
        assertEquals(PrizeSpinKind.Gold, wheel.nextKind)
        assertTrue(wheel.guaranteedJackpot)
        assertTrue(wheel.goldSlices.last().isJackpot)
        assertEquals(5.0, wheel.purchaseMinTotal)
        assertEquals(100, wheel.recent.single().pixels)
    }

    @Test
    fun `sem roleta girando a roleta e nula`() = runTest {
        val api = FakeApi().clientReturning("""{"success": true, "data": {"wheel": null}}""")

        assertNull((PrizeWheelRepository(api).current() as Outcome.Success).value)
    }

    @Test
    fun `o giro vai por POST e traz a fatia e o estado novo`() = runTest {
        val fake = FakeApi()
        val result = PrizeWheelRepository(fake.clientReturning("""
            {"success": true, "data": {"kind": "gold", "slice_index": 1, "label": "Um energético por nossa conta", "pixels": 1200,
             "is_jackpot": true, "forced": true, "state": $wheelJson}}
        """.trimIndent())).spin()

        val spin = (result as Outcome.Success).value

        assertEquals(HttpMethod.Post, fake.lastRequest?.method)
        assertTrue(fake.lastRequest?.url?.encodedPath.orEmpty().endsWith("prize-wheel/spin"))
        assertEquals(1, spin.sliceIndex)
        assertTrue(spin.isBig)
        assertEquals(12, spin.wheel?.goldSpins)
    }

    @Test
    fun `girar sem giro devolve a mensagem do servidor`() = runTest {
        val api = FakeApi(HttpStatusCode.UnprocessableEntity).clientReturning(
            """{"success": false, "error": {"code": "prize_wheel_unavailable", "message": "Você não tem giros agora."}}""",
        )

        val failure = PrizeWheelRepository(api).spin() as Outcome.Failure

        assertEquals("Você não tem giros agora.", failure.error.message)
    }
}
