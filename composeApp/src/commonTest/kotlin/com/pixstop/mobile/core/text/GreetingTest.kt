package com.pixstop.mobile.core.text

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {

    @Test
    fun `manha comeca as cinco e vai ate o meio-dia`() {
        assertEquals("Bom dia", greetingForHour(5))
        assertEquals("Bom dia", greetingForHour(11))
    }

    @Test
    fun `tarde comeca ao meio-dia`() {
        assertEquals("Boa tarde", greetingForHour(12))
        assertEquals("Boa tarde", greetingForHour(17))
    }

    @Test
    fun `noite comeca as dezoito e cobre a madrugada`() {
        assertEquals("Boa noite", greetingForHour(18))
        assertEquals("Boa noite", greetingForHour(23))
        assertEquals("Boa noite", greetingForHour(0))
        assertEquals("Boa noite", greetingForHour(4))
    }
}
