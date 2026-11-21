package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.support.FakeApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpiryRepositoryTest {

    @Test
    fun `a validade chega por lugar e a retirada manda o lugar`() = runTest {
        val json = """
            {"success": true, "data": {"places": [{"appliance_id": null, "name": "Depósito", "expired_units": 2,
              "lots": [{"product_id": 7, "product": "Pudim", "expires_on": "2026-11-18", "quantity": 2, "days_left": -2, "expired": true, "warning": true}],
              "undated": [{"product_id": 8, "product": "Brownie", "quantity": 1}]}],
             "summary": {"expired_units": 2, "warning_units": 0, "undated_units": 1}}}
        """.trimIndent()

        val overview = (ExpiryRepository(FakeApi().clientReturning(json)).overview() as Outcome.Success).value

        assertEquals("Depósito", overview.places.single().name)
        assertEquals(null, overview.places.single().applianceId)
        assertEquals(-2, overview.places.single().lots.single().daysLeft)
        assertEquals(1, overview.undatedUnits)

        val api = FakeApi()
        val outcome = ExpiryRepository(api.clientReturning("""{"success": true, "data": null, "message": "ok"}""")).removeExpired(3)

        assertTrue(outcome is Outcome.Success)
        assertEquals("{\"appliance_id\":3}", api.lastBody.replace(" ", ""))
    }
}
