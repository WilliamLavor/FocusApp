package com.example.focusapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusSessaoTest {

    @Test
    fun rotulosConhecidos() {
        assertEquals("Concluído", StatusSessao.rotuloDe("CONCLUIDO"))
        assertEquals("Abandonado", StatusSessao.rotuloDe("ABANDONADO"))
    }

    @Test
    fun nomeSalvoNoBancoBateComOEnum() {
        assertEquals("CONCLUIDO", StatusSessao.CONCLUIDO.name)
        assertEquals("ABANDONADO", StatusSessao.ABANDONADO.name)
    }

    @Test
    fun textoDesconhecidoVoltaComoVeio() {
        assertEquals("OUTRO", StatusSessao.rotuloDe("OUTRO"))
    }
}
