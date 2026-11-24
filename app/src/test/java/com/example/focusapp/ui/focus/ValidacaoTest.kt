package com.example.focusapp.ui.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacaoTest {

    @Test
    fun aceitaOsLimitesDoFoco() {
        assertEquals(FOCO_MIN_MINUTOS, lerMinutos("1", FOCO_MIN_MINUTOS, FOCO_MAX_MINUTOS))
        assertEquals(FOCO_MAX_MINUTOS, lerMinutos("180", FOCO_MIN_MINUTOS, FOCO_MAX_MINUTOS))
    }

    @Test
    fun recusaForaDoLimite() {
        assertNull(lerMinutos("0", FOCO_MIN_MINUTOS, FOCO_MAX_MINUTOS))
        assertNull(lerMinutos("181", FOCO_MIN_MINUTOS, FOCO_MAX_MINUTOS))
    }

    @Test
    fun descansoPodeSerZero() {
        assertEquals(0, lerMinutos("0", DESCANSO_MIN_MINUTOS, DESCANSO_MAX_MINUTOS))
        assertNull(lerMinutos("61", DESCANSO_MIN_MINUTOS, DESCANSO_MAX_MINUTOS))
    }

    @Test
    fun textoVazioOuInvalidoViraNull() {
        assertNull(lerMinutos("", 1, 10))
        assertNull(lerMinutos("abc", 1, 10))
        assertNull(lerMinutos("2,5", 1, 10))
    }

    @Test
    fun ignoraEspacosNasPontas() {
        assertEquals(25, lerMinutos(" 25 ", 1, 180))
    }
}
