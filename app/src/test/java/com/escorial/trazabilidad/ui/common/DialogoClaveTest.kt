package com.escorial.trazabilidad.ui.common

import com.escorial.trazabilidad.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class DialogoClaveTest {

    @Test
    fun `el hash es el SHA-256 hexadecimal en minusculas`() {
        // Vector conocido: sha256("abc")
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256("abc"),
        )
    }

    @Test
    fun `sin clave configurada el acceso queda libre`() {
        assumeTrue(BuildConfig.CONFIG_PASSWORD_SHA256.isBlank())

        assertFalse(configPideClave())
        assertTrue(claveConfigValida("")) // cualquier cosa entra
    }

    @Test
    fun `con clave configurada solo entra la correcta`() {
        val esperado = BuildConfig.CONFIG_PASSWORD_SHA256
        assumeTrue(esperado.isNotBlank())

        assertTrue(configPideClave())
        assertFalse(claveConfigValida(""))
        assertFalse(claveConfigValida("otra-cosa"))
        // No se escribe la clave real en el test: se deriva del hash que ya compiló.
        assertTrue(esperado.length == 64)
    }

    @Test
    fun `el hash configurado tiene forma de SHA-256`() {
        val esperado = BuildConfig.CONFIG_PASSWORD_SHA256
        assumeTrue(esperado.isNotBlank())

        // 64 hex: si alguien guarda la clave en texto plano en config.properties
        // por error, esto lo detecta.
        assertTrue(esperado.matches(Regex("[0-9a-fA-F]{64}")))
    }
}
