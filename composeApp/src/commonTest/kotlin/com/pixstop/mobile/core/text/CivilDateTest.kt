package com.pixstop.mobile.core.text

import kotlin.test.Test
import kotlin.test.assertEquals

class CivilDateTest {

    @Test
    fun `ano mes e dia saem do instante em UTC`() {
        // 2026-09-06T00:00:00Z
        val millis = IsoInstant.toEpochMillis("2026-09-06T00:00:00Z")!!

        assertEquals(Triple(2026, 9, 6), IsoInstant.civilDateOf(millis))
        assertEquals(2026 to 9, IsoInstant.yearMonthOf(millis))
    }

    @Test
    fun `o ultimo dia do ano e o primeiro do seguinte nao se confundem`() {
        assertEquals(Triple(2026, 12, 31), IsoInstant.civilDateOf(IsoInstant.toEpochMillis("2026-12-31T23:59:59Z")!!))
        assertEquals(Triple(2027, 1, 1), IsoInstant.civilDateOf(IsoInstant.toEpochMillis("2027-01-01T00:00:00Z")!!))
    }

    @Test
    fun `fevereiro bissexto`() {
        assertEquals(Triple(2028, 2, 29), IsoInstant.civilDateOf(IsoInstant.toEpochMillis("2028-02-29T12:00:00Z")!!))
    }
}
