package com.pixstop.mobile.core.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.pixstop.mobile.BuildKonfig

/**
 * Logger único do app.
 *
 * Existe para que `data/` e `core/` nunca usem `println`: em release ele silencia
 * o que é ruído e mantém o que ajuda a investigar um problema em campo.
 */
object AppLogger {

    private const val DEFAULT_TAG = "Pixstop"

    init {
        Logger.setMinSeverity(if (BuildKonfig.DEBUG) Severity.Debug else Severity.Info)
    }

    fun withTag(tag: String): Logger = Logger.withTag(tag)

    fun d(message: String, tag: String = DEFAULT_TAG) = Logger.withTag(tag).d(message)

    fun i(message: String, tag: String = DEFAULT_TAG) = Logger.withTag(tag).i(message)

    fun w(message: String, tag: String = DEFAULT_TAG) = Logger.withTag(tag).w(message)

    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        if (throwable != null) {
            Logger.withTag(tag).e(throwable) { message }
        } else {
            Logger.withTag(tag).e(message)
        }
    }
}
