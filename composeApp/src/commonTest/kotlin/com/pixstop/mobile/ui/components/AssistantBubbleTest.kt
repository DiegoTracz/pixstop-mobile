package com.pixstop.mobile.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AssistantBubbleTest {

    @Test
    fun soltoNaMetadeEsquerdaEncostaNaEsquerda() {
        assertEquals(0f to 300f, snapBubble(x = 120f, y = 300f, maxX = 400f, maxY = 900f))
    }

    @Test
    fun soltoNaMetadeDireitaEncostaNaDireita() {
        assertEquals(400f to 300f, snapBubble(x = 250f, y = 300f, maxX = 400f, maxY = 900f))
    }

    @Test
    fun naoSaiDaTelaPorCimaNemPorBaixo() {
        assertEquals(400f to 0f, snapBubble(x = 390f, y = -50f, maxX = 400f, maxY = 900f))
        assertEquals(0f to 900f, snapBubble(x = 10f, y = 1200f, maxX = 400f, maxY = 900f))
    }
}
