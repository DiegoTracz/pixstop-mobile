package com.pixstop.mobile.domain.auth

/**
 * A regra de senha do servidor em produção (`Password::defaults()` no
 * AppServiceProvider): 12 caracteres, maiúscula, minúscula, número e
 * símbolo. A checagem de senha vazada só o servidor faz.
 */
enum class PasswordRule(
    val label: String,
    val error: String,
    private val check: (String) -> Boolean,
) {
    MinLength("12+ caracteres", "A senha precisa de no mínimo 12 caracteres.", { it.length >= 12 }),
    Uppercase("Maiúscula", "Inclua ao menos uma letra maiúscula.", { p -> p.any { it.isUpperCase() } }),
    Lowercase("Minúscula", "Inclua ao menos uma letra minúscula.", { p -> p.any { it.isLowerCase() } }),
    Digit("Número", "Inclua ao menos um número.", { p -> p.any { it.isDigit() } }),

    /** Mesmo critério do Laravel: qualquer coisa que não seja letra nem número. */
    Symbol("Símbolo", "Inclua ao menos um símbolo (ex.: ! @ # \$).", { p -> p.any { !it.isLetterOrDigit() } }),
    ;

    fun passes(password: String): Boolean = check(password)

    companion object {
        fun firstFailure(password: String): PasswordRule? = entries.firstOrNull { !it.passes(password) }
    }
}
