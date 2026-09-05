package com.pixstop.mobile.core.text

/**
 * Converte o HTML dos documentos legais em texto legível.
 *
 * O mesmo conteúdo alimenta o site, então chega marcado. Renderizar HTML de
 * verdade exigiria uma implementação por plataforma; o que o aceite precisa é
 * que a pessoa consiga ler — daí texto puro, preservando parágrafos e itens
 * de lista.
 */
object HtmlText {

    private val entities = mapOf(
        "&nbsp;" to " ",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&apos;" to "'",
        "&ordm;" to "º",
        "&ordf;" to "ª",
        "&mdash;" to "—",
        "&ndash;" to "–",
        "&hellip;" to "…",
        // Por último: trocar `&amp;` antes desfaria as outras entidades.
        "&amp;" to "&",
    )

    /** Tags que separam blocos: viram quebra de linha. */
    private val blockTags = Regex(
        "</?(p|div|br|h[1-6]|ul|ol|tr|table|section|article|blockquote)[^>]*>",
        RegexOption.IGNORE_CASE,
    )

    private val listItem = Regex("<li[^>]*>", RegexOption.IGNORE_CASE)
    private val anyTag = Regex("<[^>]+>")
    private val blankRuns = Regex("\n{3,}")
    private val trailingSpaces = Regex("[ \t]+\n")

    fun toPlainText(html: String): String {
        var text = html
            .replace(listItem, "\n• ")
            .replace(blockTags, "\n")
            .replace(anyTag, "")

        entities.forEach { (entity, char) -> text = text.replace(entity, char) }

        return text
            .replace("\r\n", "\n")
            .replace(trailingSpaces, "\n")
            .replace(blankRuns, "\n\n")
            .trim()
    }
}
