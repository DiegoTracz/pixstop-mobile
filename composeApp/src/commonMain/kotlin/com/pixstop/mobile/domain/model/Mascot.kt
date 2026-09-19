package com.pixstop.mobile.domain.model

/**
 * O mascote da Pixelstop: o LED RGB da fita, com cara.
 *
 * Aqui mora o que ele é, como dado: os estados, as cores e o mapa de cada
 * situação para uma cara. O desenho fica em `PixelMascot`. É a mesma tabela do
 * `resources/js/lib/mascot.ts` da web; mudar uma cor ou um rosto é mudar nos
 * dois, e o `MascotTest` confere que a daqui continua dizendo o mesmo.
 *
 * As cores são as da fita, não as do tema: um LED verde é verde nos dois temas.
 * Guardadas como ARGB para o domínio não depender do Compose.
 */
object MascotColors {
    const val Brand = 0xFF5B8CFF
    const val White = 0xFFEEF2F7
    const val Red = 0xFFFF5C5C
    const val Cyan = 0xFF2EE6CB
    const val Green = 0xFF3DDC97
    const val Yellow = 0xFFFFC93C
    const val Orange = 0xFFFF8A3D
    const val Magenta = 0xFFFF4FA3
    const val Off = 0xFF3B4A66

    /** A ordem do ciclo RGB, a mesma dos programas do controle da fita. */
    val Cycle = listOf(Red, Yellow, Green, Cyan, Brand, Magenta)

    /** Cores que somem no fundo claro e pedem contorno. */
    val Pale = setOf(White)
}

enum class MascotFace { Normal, Closed, Scared, Up, Angry, Happy, Smile, Worried, Dizzy }

/** `Cycle` só existe na espera: cor ciclando não informa nada, então não serve a mais nenhum estado. */
enum class MascotEffect { None, Pulse, Blink3, Cycle }

/** O vocabulário da interface: esperar, dar certo, dar errado. */
enum class MascotState { Loading, Success, Error, Warning, Idle, Offline }

/** Os dez estados da fita da geladeira, com os nomes do `LED_IR.md`. */
enum class FitaState {
    Parado, Esperando, SemRede, Conectado, Bilhete, Liberada, Aberta, Demais, Reset, Desligado;

    companion object
}

data class MascotLook(
    val color: Long,
    val face: MascotFace,
    val label: String,
    val effect: MascotEffect = MascotEffect.None,
    /** Pixels amarelos saindo do LED: só quando uma compra libera. */
    val pixels: Boolean = false,
    /** O halo existe só com o LED aceso. */
    val lit: Boolean = true,
) {
    val isPale: Boolean get() = color in MascotColors.Pale

    companion object {
        fun of(state: MascotState): MascotLook = when (state) {
            MascotState.Loading -> MascotLook(MascotColors.Brand, MascotFace.Up, "Carregando", MascotEffect.Cycle)
            MascotState.Success -> MascotLook(MascotColors.Green, MascotFace.Happy, "Deu certo", MascotEffect.Blink3, pixels = true)
            MascotState.Error -> MascotLook(MascotColors.Red, MascotFace.Scared, "Algo deu errado", MascotEffect.Blink3)
            // O aviso é laranja, não amarelo: na paleta o amarelo é do pixel, e só a
            // tela que espelha a geladeira usa o amarelo dela.
            MascotState.Warning -> MascotLook(MascotColors.Orange, MascotFace.Worried, "Atenção")
            MascotState.Idle -> MascotLook(MascotColors.Brand, MascotFace.Normal, "Pixelstop")
            MascotState.Offline -> MascotLook(MascotColors.Off, MascotFace.Closed, "Sem conexão", lit = false)
        }

        fun of(fita: FitaState): MascotLook = when (fita) {
            FitaState.Parado -> MascotLook(MascotColors.Brand, MascotFace.Normal, "Pronta para usar")
            FitaState.Esperando -> MascotLook(MascotColors.White, MascotFace.Closed, "Esperando o app", MascotEffect.Pulse)
            FitaState.SemRede -> MascotLook(MascotColors.Red, MascotFace.Scared, "Sem internet")
            FitaState.Conectado -> MascotLook(MascotColors.Cyan, MascotFace.Up, "Celular conectado")
            FitaState.Bilhete -> MascotLook(MascotColors.Red, MascotFace.Angry, "Não foi possível abrir", MascotEffect.Blink3)
            FitaState.Liberada -> MascotLook(MascotColors.Green, MascotFace.Happy, "Compra liberada", MascotEffect.Blink3, pixels = true)
            FitaState.Aberta -> MascotLook(MascotColors.White, MascotFace.Smile, "Porta aberta")
            FitaState.Demais -> MascotLook(MascotColors.Yellow, MascotFace.Worried, "Porta aberta há muito tempo")
            FitaState.Reset -> MascotLook(MascotColors.Magenta, MascotFace.Dizzy, "Reiniciando", MascotEffect.Blink3)
            FitaState.Desligado -> MascotLook(MascotColors.Off, MascotFace.Closed, "Desligada", lit = false)
        }
    }
}

/**
 * O pedido pago espelha a fita: o que a porta mostra por cor, a tela mostra
 * pelo mesmo LED. Mesma tabela do `pickupFitaState` da web
 * (docs/plans/MASCOTE_LED.md), mais o que só o app sabe: quando o celular está
 * falando com a geladeira por Bluetooth, a fita fica ciano esperando o bilhete.
 *
 * @param windowExpired a tentativa venceu sem a porta abrir: ele volta a esperar.
 */
fun FitaState.Companion.of(
    pickup: Pickup,
    talkingByBluetooth: Boolean = false,
    windowExpired: Boolean = false,
): FitaState = when {
    talkingByBluetooth -> FitaState.Conectado
    pickup.status == PickupStatus.Unlocking && !windowExpired -> FitaState.Liberada
    pickup.status == PickupStatus.Opened -> FitaState.Aberta
    pickup.status == PickupStatus.PickedUp || pickup.status == PickupStatus.Manual -> FitaState.Parado
    pickup.reason == PickupReason.Busy -> FitaState.Demais
    pickup.reason == PickupReason.Offline -> FitaState.SemRede
    pickup.reason == PickupReason.Exhausted -> FitaState.Bilhete
    pickup.status == PickupStatus.None -> FitaState.Parado
    else -> FitaState.Conectado
}
