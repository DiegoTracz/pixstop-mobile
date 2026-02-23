package com.pixstop.mobile.core.config

/**
 * Utilitário para extrair o código de empresa de URLs do PixStop.
 *
 * Suporta URLs no formato:
 * - https://pixstop.com.br/register/user?code=CONFIALMQYNV
 * - pixstop.com.br/register/user?code=CONFIALMQYNV
 * - Texto puro do código (fallback)
 */
object CompanyCodeParser {

    /**
     * Extrai o company_code de uma URL ou texto escaneado.
     *
     * @param rawValue O valor bruto escaneado do QR code
     * @return O código extraído, ou o valor bruto se não for uma URL reconhecida
     */
    fun parse(rawValue: String): String {
        val trimmed = rawValue.trim()

        // Tenta extrair ?code= da URL
        val codeFromUrl = extractQueryParam(trimmed, "code")
        if (!codeFromUrl.isNullOrBlank()) {
            return codeFromUrl
        }

        // Se não é URL, retorna o valor bruto como código direto
        return trimmed
    }

    private fun extractQueryParam(url: String, param: String): String? {
        // Procura por ?code= ou &code=
        val patterns = listOf("?$param=", "&$param=")
        for (pattern in patterns) {
            val index = url.indexOf(pattern, ignoreCase = true)
            if (index >= 0) {
                val valueStart = index + pattern.length
                val valueEnd = url.indexOf('&', valueStart).let {
                    if (it < 0) url.length else it
                }
                val value = url.substring(valueStart, valueEnd).trim()
                if (value.isNotBlank()) return value
            }
        }
        return null
    }
}

