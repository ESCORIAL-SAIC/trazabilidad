package com.escorial.trazabilidad.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionesTest {

    @Test
    fun `una version numerica lleva prefijo v`() {
        assertEquals("App v1.0.2-dev · API v1.2.3", versionesTexto("1.0.2-dev", "1.2.3"))
    }

    @Test
    fun `una version no numerica no lleva prefijo v para no leerse como API vdev`() {
        assertEquals("API dev", apiVersionLabel("dev"))
    }

    @Test
    fun `sin verificar y desconocida se distinguen entre si`() {
        assertEquals("API sin verificar", apiVersionLabel(null))
        assertEquals("API sin verificar", apiVersionLabel(""))
        assertEquals("API (versión desconocida)", apiVersionLabel(VERSION_DESCONOCIDA))
    }
}
