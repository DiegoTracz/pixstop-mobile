package com.pixstop.mobile.core.platform

import kotlin.test.Test
import kotlin.test.assertFalse

class StorePolicyTest {

    @Test
    fun `o iPhone nao vende pixels dentro do app`() {
        assertFalse(sellsPixelsInApp)
    }
}
