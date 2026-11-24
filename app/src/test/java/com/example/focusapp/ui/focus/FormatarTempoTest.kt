package com.example.focusapp.ui.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatarTempoTest {

    @Test
    fun minutosESegundos() {
        assertEquals("00:00", formatarTempo(0))
        assertEquals("01:05", formatarTempo(65))
        assertEquals("25:00", formatarTempo(25 * 60))
    }

    @Test
    fun comHoras() {
        assertEquals("1:02:05", formatarTempo(3725))
        assertEquals("3:00:00", formatarTempo(3 * 3600))
    }
}
