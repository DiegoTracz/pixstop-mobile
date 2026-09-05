package com.pixstop.mobile.core.text

/**
 * Converte o instante ISO-8601 que a API devolve em epoch de milissegundos.
 *
 * Existe porque o app não tem uma biblioteca de data e hora, e a única conta
 * que a loja precisa é quanto falta para a reserva do carrinho vencer. Aceita
 * o formato que o Laravel produz (`2026-09-05T13:34:32+00:00`), com `Z` ou
 * deslocamento, e com ou sem fração de segundo.
 */
object IsoInstant {

    private val pattern = Regex(
        """^(\d{4})-(\d{2})-(\d{2})[Tt ](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?(Z|z|[+-]\d{2}:?\d{2})?$""",
    )

    /** @return o instante em milissegundos, ou `null` se o texto não for uma data. */
    fun toEpochMillis(value: String?): Long? {
        val match = pattern.matchEntire(value?.trim().orEmpty()) ?: return null
        val (year, month, day, hour, minute, second, offset) = match.destructured

        val days = daysFromCivil(year.toInt(), month.toInt(), day.toInt())
        val seconds = days * 86_400L + hour.toInt() * 3_600L + minute.toInt() * 60L + second.toInt()

        return (seconds - offsetSeconds(offset)) * 1_000L
    }

    /**
     * Deslocamento do fuso em segundos. Ausente ou `Z` significa UTC.
     */
    private fun offsetSeconds(offset: String): Long {
        if (offset.isEmpty() || offset.equals("Z", ignoreCase = true)) {
            return 0
        }

        val digits = offset.drop(1).replace(":", "")
        val hours = digits.take(2).toLong()
        val minutes = digits.drop(2).take(2).toLong()
        val total = hours * 3_600L + minutes * 60L

        return if (offset.startsWith('-')) -total else total
    }

    /**
     * Dias desde 1970-01-01 no calendário gregoriano.
     *
     * É o algoritmo `days_from_civil` de Howard Hinnant, que acerta anos
     * bissextos sem tabela nem laço.
     */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yearOfEra = y - era * 400
        val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365L + yearOfEra / 4 - yearOfEra / 100 + dayOfYear

        return era * 146_097L + dayOfEra - 719_468L
    }
}

private operator fun <T> List<T>.component6(): T = this[5]
private operator fun <T> List<T>.component7(): T = this[6]
