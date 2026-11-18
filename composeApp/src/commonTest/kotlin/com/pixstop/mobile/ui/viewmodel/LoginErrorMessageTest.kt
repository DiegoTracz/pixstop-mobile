package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginErrorMessageTest {

    @Test
    fun aWrongPasswordIsNotAnExpiredSession() {
        assertEquals("E-mail ou senha incorretos.", loginErrorMessage(DomainError.Unauthorized()))
    }

    @Test
    fun otherErrorsKeepTheirMessage() {
        assertEquals("Muitas tentativas.", loginErrorMessage(DomainError.RateLimited("Muitas tentativas.")))
    }
}
