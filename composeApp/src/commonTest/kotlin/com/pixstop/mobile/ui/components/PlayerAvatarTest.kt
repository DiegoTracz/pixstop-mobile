package com.pixstop.mobile.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * As iniciais são as mesmas da web: primeira letra do primeiro e do último
 * nome. Divergir aqui faria o mesmo usuário aparecer com siglas diferentes no
 * site e no app.
 */
class PlayerAvatarTest {

    @Test
    fun `usa a primeira letra do primeiro e do ultimo nome`() {
        assertEquals("CS", initialsOf("Carlos Silva"))
        assertEquals("CJ", initialsOf("Carlos Silva Jr"))
    }

    @Test
    fun `nome unico usa so a primeira letra`() {
        assertEquals("C", initialsOf("Carlos"))
    }

    @Test
    fun `espacos sobrando nao viram iniciais vazias`() {
        assertEquals("CS", initialsOf("  Carlos   Silva  "))
    }

    @Test
    fun `nome vazio nao quebra`() {
        assertEquals("", initialsOf(""))
        assertEquals("", initialsOf("   "))
    }
}
