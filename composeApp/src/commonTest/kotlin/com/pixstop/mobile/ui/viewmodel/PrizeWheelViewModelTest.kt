package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.data.repository.PrizeWheelRepository
import com.pixstop.mobile.support.FakeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O giro em dois tempos: o servidor sorteia e só quando a roda para o
 * resultado aparece e os giros diminuem.
 */
class PrizeWheelViewModelTest {

    private fun wheel(gold: Int) = """
        {"id": 3, "name": "Roleta", "available": {"gold": $gold, "purchase": 0},
         "slices": {"gold": [{"label": "50 moedas", "pixels": 50}, {"label": "100 moedas", "pixels": 100}], "purchase": []}}
    """.trimIndent()

    private fun wheelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private suspend fun PrizeWheelViewModel.awaitState(predicate: (PrizeWheelUiState) -> Boolean): PrizeWheelUiState =
        withContext(Dispatchers.Default) { withTimeout(5_000) { uiState.first(predicate) } }

    @Test
    fun `o resultado so aparece quando a roda para`() = wheelTest {
        val api = FakeApi()
        api.nextBody = """{"success": true, "data": {"wheel": ${wheel(2)}}}"""
        val model = PrizeWheelViewModel(PrizeWheelRepository(api.clientReturning("""{"success": true, "data": {"wheel": ${wheel(2)}}}""")))
        model.awaitState { it.wheel != null }

        api.nextBody = """{"success": true, "data": {"kind": "gold", "slice_index": 1, "label": "100 moedas", "pixels": 100, "is_jackpot": false, "state": ${wheel(1)}}}"""
        model.spin()

        val spinning = model.awaitState { it.pending != null }
        assertTrue(spinning.isSpinning)
        assertNull(spinning.lastResult)
        assertEquals(2, spinning.wheel?.goldSpins)

        model.onSpinAnimated()

        val stopped = model.uiState.value
        assertEquals(false, stopped.isSpinning)
        assertEquals(100, stopped.lastResult?.pixels)
        assertEquals(1, stopped.wheel?.goldSpins)
    }

    @Test
    fun `sem giros o botao nao pede nada ao servidor`() = wheelTest {
        val api = FakeApi()
        val model = PrizeWheelViewModel(PrizeWheelRepository(api.clientReturning("""{"success": true, "data": {"wheel": ${wheel(0)}}}""")))
        model.awaitState { it.wheel != null }
        val calls = api.lastRequest

        model.spin()

        assertEquals(calls, api.lastRequest)
        assertEquals(false, model.uiState.value.isSpinning)
    }
}
