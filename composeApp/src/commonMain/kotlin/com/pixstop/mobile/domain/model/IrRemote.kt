package com.pixstop.mobile.domain.model

/**
 * O controle da fita LED, como ele é na mão (Fase 9.10, etapa A).
 *
 * Quem instala a geladeira tem o controle de 24 teclas em cima da mesa, e é
 * apertando as teclas que se descobre o que cada uma faz naquela fita. O app
 * desenha o mesmo controle: a pessoa aperta, a geladeira acende, e a cor que
 * ficou acesa é a que se salva.
 *
 * Os códigos infravermelhos não aparecem aqui. Eles são do modelo do
 * controle, moram no catálogo do admin central e quem os resolve é o
 * servidor — o app manda o nome da tecla.
 */
data class RemoteKey(
    /** O nome que o servidor conhece: `red`, `power_on`, `flash`. */
    val slug: String,
    val label: String,
    /** O que se lê na tecla, quando ela tem texto. */
    val text: String? = null,
    /** A cor da tecla no desenho; nulo é tecla cinza de programa. */
    val swatch: Long? = null,
) {
    /** Só as teclas que o servidor aceita como repouso viram escolha. */
    fun isRestingColor(colors: List<LedColorOption>): Boolean = colors.any { it.value == slug }
}

/**
 * As seis linhas de quatro teclas, na ordem física do controle.
 *
 * A disposição é a mesma do desenho da web (`IrRemote.vue`): quem tem o
 * controle na mão reconhece sem procurar. Mudou aqui, muda lá.
 */
object IrRemoteLayout {
    val rows: List<List<RemoteKey>> = listOf(
        listOf(
            RemoteKey("brightness_up", "Mais brilho", "+", 0xFFF8FAFC),
            RemoteKey("brightness_down", "Menos brilho", "−", 0xFFF8FAFC),
            RemoteKey("power_off", "Desligar", "OFF", 0xFF1F2937),
            RemoteKey("power_on", "Ligar", "ON", 0xFFDC2626),
        ),
        listOf(
            RemoteKey("red", "Vermelho", "R", 0xFFEF4444),
            RemoteKey("green", "Verde", "G", 0xFF16A34A),
            RemoteKey("blue", "Azul", "B", 0xFF2563EB),
            RemoteKey("white", "Branco", "W", 0xFFF8FAFC),
        ),
        listOf(
            RemoteKey("red_1", "Vermelho 1", null, 0xFFF43F2A),
            RemoteKey("green_1", "Verde 1", null, 0xFF22C55E),
            RemoteKey("blue_1", "Azul 1", null, 0xFF38BDF8),
            RemoteKey("flash", "Flash", "FLASH"),
        ),
        listOf(
            RemoteKey("red_2", "Vermelho 2", null, 0xFFF97316),
            RemoteKey("green_2", "Verde 2", null, 0xFF14B8A6),
            RemoteKey("blue_2", "Azul 2", null, 0xFF7C3AED),
            RemoteKey("strobe", "Strobe", "STROBE"),
        ),
        listOf(
            RemoteKey("red_3", "Vermelho 3", null, 0xFFFB923C),
            RemoteKey("green_3", "Verde 3", null, 0xFF06B6D4),
            RemoteKey("blue_3", "Azul 3", null, 0xFFA21CAF),
            RemoteKey("fade", "Fade", "FADE"),
        ),
        listOf(
            RemoteKey("yellow", "Amarelo", null, 0xFFEAB308),
            RemoteKey("cyan", "Ciano", null, 0xFF0891B2),
            RemoteKey("magenta", "Magenta", null, 0xFFD946EF),
            RemoteKey("smooth", "Smooth", "SMOOTH"),
        ),
    )

    val keys: List<RemoteKey> = rows.flatten()
}

/** Uma cor que serve de repouso, como o servidor a oferece. */
data class LedColorOption(val value: String, val label: String, val swatch: String)

/** Um modelo de controle do catálogo, e as teclas que alguém já mapeou nele. */
data class IrProfile(val id: Long, val name: String, val keys: List<String>)

/**
 * O que a geladeira mostra quando não está acontecendo nada, e com qual
 * controle ela fala.
 */
data class LedSettings(
    val color: String,
    val profileId: Long?,
    val online: Boolean,
    val colors: List<LedColorOption>,
    val profiles: List<IrProfile>,
) {
    /** As teclas que o controle escolhido tem código para mandar. */
    val availableKeys: List<String>
        get() = profiles.firstOrNull { it.id == profileId }?.keys.orEmpty()

    val colorLabel: String
        get() = colors.firstOrNull { it.value == color }?.label ?: color

    /** Sem controle escolhido não há tecla nenhuma para apertar. */
    val canPress: Boolean get() = online && profileId != null

    companion object {
        val Empty = LedSettings(color = "fade", profileId = null, online = false, colors = emptyList(), profiles = emptyList())
    }
}
