package com.pixstop.mobile.domain.model

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A roda desenhada (docs/plans/LANCAMENTO_LOCARMAIS.md no servidor): 16 fatias
 * em vez de uma por prêmio. Com uma fatia por prêmio, o prêmio maior ocupava
 * um quarto da roda e parecia sair 1 vez em 4, quando sai 1 em 50. Aqui cada
 * prêmio repete conforme o peso, o prêmio maior aparece uma vez só, e as
 * repetições ficam espalhadas. O sorteio não muda: o servidor diz o prêmio, e
 * a roda para numa das fatias dele. É a mesma conta da web
 * (resources/js/lib/prizeWheelRing.ts no servidor).
 */
object PrizeWheelRing {
    const val SIZE = 16

    /** Cada posição da roda aponta para um prêmio da lista. */
    fun build(slices: List<PrizeWheelSlice>, size: Int = SIZE): List<Int> {
        if (slices.isEmpty()) return emptyList()

        val total = max(size, slices.size)
        val jackpots = slices.indices.filter { slices[it].isJackpot }
        val others = slices.indices.filterNot { slices[it].isJackpot }
        val room = total - jackpots.size
        val weight = others.sumOf { max(0.0, slices[it].weight) }

        val counts = mutableMapOf<Int, Int>()
        jackpots.forEach { counts[it] = 1 }
        others.forEach { index ->
            val share = if (weight > 0) max(0.0, slices[index].weight) / weight * room else room.toDouble() / others.size
            counts[index] = max(1, share.roundToInt())
        }

        // Acerta o total: tira do que mais repete, põe no de maior peso.
        fun othersSum() = others.sumOf { counts[it] ?: 0 }
        while (others.isNotEmpty() && othersSum() > room) {
            val largest = others.maxBy { counts[it] ?: 0 }
            if ((counts[largest] ?: 0) <= 1) break
            counts[largest] = (counts[largest] ?: 0) - 1
        }
        while (others.isNotEmpty() && othersSum() < room) {
            val heaviest = others.maxBy { slices[it].weight }
            counts[heaviest] = (counts[heaviest] ?: 0) + 1
        }

        // Espalha por revezamento ponderado: a cada posição, cada prêmio
        // ganha crédito pelo quanto repete, e sai o de maior crédito. As
        // repetições se intercalam, sem três iguais seguidas. Como na web.
        val prizes = counts.keys.sorted()
        val credit = prizes.associateWith { 0 }.toMutableMap()

        return (0 until total).map {
            prizes.forEach { prize -> credit[prize] = (credit[prize] ?: 0) + (counts[prize] ?: 0) }
            val next = prizes.reduce { best, prize -> if ((credit[prize] ?: 0) > (credit[best] ?: 0)) prize else best }
            credit[next] = (credit[next] ?: 0) - total
            next
        }
    }

    /** Uma das fatias do prêmio sorteado, ao acaso, para a roda parar nela. */
    fun slotFor(ring: List<Int>, prize: Int, random: Random = Random): Int {
        val slots = ring.indices.filter { ring[it] == prize }
        return if (slots.isEmpty()) 0 else slots[random.nextInt(slots.size)]
    }
}
