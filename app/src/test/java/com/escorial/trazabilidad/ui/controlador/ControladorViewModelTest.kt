package com.escorial.trazabilidad.ui.controlador

import com.escorial.trazabilidad.data.api.dto.CampoBarralDto
import com.escorial.trazabilidad.data.api.dto.EtiquetaDto
import com.escorial.trazabilidad.data.api.dto.PuestoAsignadoDto
import com.escorial.trazabilidad.data.api.dto.ResolverResponse
import com.escorial.trazabilidad.data.api.dto.ValidarFrontalRequest
import com.escorial.trazabilidad.data.api.dto.ValidarFrontalResponse
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import com.escorial.trazabilidad.domain.FlujoActual
import com.escorial.trazabilidad.domain.PUESTO_CONTROL_FINAL
import com.escorial.trazabilidad.ui.config.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

private const val PRODUCTO_ID = "prod-uuid"

/** Respuesta del resolver como la manda el backend para el puesto indicado. */
private fun resolverEn(puesto: String) = ResolverResponse(
    accion = "CONTROLADOR",
    etiqueta = EtiquetaDto(numero = 1456778, producto_id = PRODUCTO_ID),
    puestoAsignado = PuestoAsignadoDto(id = "p3", nombre = puesto, c = 3),
    campoBarral = CampoBarralDto(visible = true, conCamara = true, prompt = "Codigo Frontal"),
)

/** Error de negocio del backend: 400 con {"error": "..."} en el cuerpo. */
private fun errorNegocio(mensaje: String) = HttpException(
    Response.error<Any>(400, """{"error":"$mensaje"}""".toResponseBody("application/json".toMediaType())),
)

class ControladorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = mockk<TrazabilidadRepository>()

    @After
    fun tearDown() {
        // FlujoActual es un singleton global: sin esto un test contamina al siguiente.
        FlujoActual.limpiar()
        FlujoActual.tipoProducto = "COCINA"
    }

    private fun crearViewModel(puesto: String = PUESTO_CONTROL_FINAL): ControladorViewModel {
        FlujoActual.resolver = resolverEn(puesto)
        return ControladorViewModel(repo = repo)
    }

    @Test
    fun `en Control Final los botones arrancan bloqueados hasta validar la grafica`() = runTest {
        val viewModel = crearViewModel()

        val estado = viewModel.state.value
        assertTrue(estado.esControlFinal)
        assertFalse(estado.graficaValidada)
    }

    @Test
    fun `en los demas puestos la grafica no se valida y los botones estan habilitados`() = runTest {
        val viewModel = crearViewModel(puesto = "Control de Fuga y retencion de horno")

        val estado = viewModel.state.value
        assertFalse(estado.esControlFinal)
        assertTrue(estado.graficaValidada)
    }

    @Test
    fun `si la grafica coincide se habilitan los botones y se recuerda el codigo`() = runTest {
        coEvery { repo.validarFrontal(any()) } returns ValidarFrontalResponse(valido = true)
        val viewModel = crearViewModel()

        viewModel.validarGrafica("CB123")

        val estado = viewModel.state.value
        assertTrue(estado.graficaValidada)
        assertEquals("CB123", estado.graficaOk)
        assertNull(estado.graficaRechazo)
        assertFalse(estado.validandoGrafica)
        coVerify { repo.validarFrontal(ValidarFrontalRequest("CB123", PRODUCTO_ID)) }
    }

    @Test
    fun `si la grafica no coincide se rechaza con el mensaje del backend y se permite reintentar`() = runTest {
        val mensaje = "La grafica pickeada no coincide con el producto. Codigo leido: CB999."
        coEvery { repo.validarFrontal(any()) } throws errorNegocio(mensaje)
        val viewModel = crearViewModel()

        viewModel.validarGrafica("CB999")

        val estado = viewModel.state.value
        assertFalse(estado.graficaValidada)
        assertNull(estado.graficaOk)
        assertEquals(mensaje, estado.graficaRechazo)
        // intentoGrafica es la clave que vuelve a enfocar el campo en la pantalla.
        assertEquals(1, estado.intentoGrafica)
    }

    @Test
    fun `tras un rechazo se puede volver a pickear y validar bien`() = runTest {
        coEvery { repo.validarFrontal(any()) } throws errorNegocio("no coincide")
        val viewModel = crearViewModel()
        viewModel.validarGrafica("CB999")
        viewModel.limpiarRechazoGrafica()

        coEvery { repo.validarFrontal(any()) } returns ValidarFrontalResponse(valido = true)
        viewModel.validarGrafica("CB123")

        val estado = viewModel.state.value
        assertTrue(estado.graficaValidada)
        assertEquals("CB123", estado.graficaOk)
        assertNull(estado.graficaRechazo)
    }

    @Test
    fun `un codigo vacio no llega a la API`() = runTest {
        val viewModel = crearViewModel()

        viewModel.validarGrafica("")

        coVerify(exactly = 0) { repo.validarFrontal(any()) }
        assertFalse(viewModel.state.value.graficaValidada)
    }
}
