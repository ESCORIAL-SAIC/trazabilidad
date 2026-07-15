package com.escorial.trazabilidad.ui.config

import com.escorial.trazabilidad.data.api.dto.PlantaDto
import com.escorial.trazabilidad.data.api.dto.PuestoDto
import com.escorial.trazabilidad.data.local.ConfiguracionStore
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import com.escorial.trazabilidad.domain.ConfiguracionPuesto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Pone Dispatchers.Main en un dispatcher "unconfined" durante el test.
 * ConfiguracionViewModel usa viewModelScope (que corre en Dispatchers.Main), y en un
 * unit test de JVM no existe un Main real de Android -> sin esto, viewModelScope.launch
 * explota con "Module with the Main dispatcher had failed to initialize".
 * Unconfined además hace que las corutinas corran de inmediato, sin async real,
 * así después de llamar a un metodo del ViewModel el estado ya está actualizado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private val CONFIG_POR_DEFECTO = ConfiguracionPuesto()

private val PUESTOS_COCINA = listOf(
    PuestoDto(puestocontrol_id = "1", puestocontrol_n = "Armado", puestocontrol_c = "1"),
    PuestoDto(puestocontrol_id = "2", puestocontrol_n = "Pintura", puestocontrol_c = "2"),
)

private val PLANTAS = listOf(
    PlantaDto(id = "25demayo", nombre = "25 de Mayo"),
    PlantaDto(id = "suipacha", nombre = "Suipacha"),
)

class ConfiguracionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // mockk crea un doble de la clase real sin ejecutar su constructor: ConfiguracionStore
    // pide un Context de Android que en un unit test de JVM no existe, pero como nunca
    // llamamos al codigo real (todo se stubea con every/coEvery) no hace falta.
    private val store = mockk<ConfiguracionStore>()
    private val repo = mockk<TrazabilidadRepository>()

    @Before
    fun setUp() {
        coEvery { repo.plantas() } returns PLANTAS
        coEvery { repo.puestos(any()) } returns PUESTOS_COCINA
        coEvery { store.guardar(any()) } returns Unit
    }

    private fun crearViewModel(config: ConfiguracionPuesto = CONFIG_POR_DEFECTO): ConfiguracionViewModel {
        every { store.configuracion } returns flowOf(config)
        return ConfiguracionViewModel(store = store, repo = repo)
    }

    @Test
    fun `al iniciar carga la configuracion persistida y los puestos`() = runTest {
        val config = CONFIG_POR_DEFECTO.copy(serverUrl = "http://servidor-test/", tipo = "TERMOTANQUE")
        val viewModel = crearViewModel(config)

        val estado = viewModel.state.value
        assertEquals("http://servidor-test/", estado.serverUrl)
        assertEquals("TERMOTANQUE", estado.tipo)
        assertEquals(PUESTOS_COCINA, estado.puestos)
        assertEquals(PLANTAS, estado.plantas)
        assertFalse(estado.cargando)
        assertNull(estado.error)
    }

    @Test
    fun `si el indice de puesto persistido esta fuera de rango, vuelve al puesto 0`() = runTest {
        val config = CONFIG_POR_DEFECTO.copy(puestoIndex = 99)

        val viewModel = crearViewModel(config)

        assertEquals(0, viewModel.state.value.puestoIndex)
    }

    @Test
    fun `si falla la carga de puestos, muestra error y deja de cargar`() = runTest {
        coEvery { repo.puestos(any()) } throws RuntimeException("sin conexion")

        val viewModel = crearViewModel()

        val estado = viewModel.state.value
        assertFalse(estado.cargando)
        assertNotNull(estado.error)
        assertTrue(estado.puestos.isEmpty())
    }

    @Test
    fun `si falla la carga de plantas, el error se ignora a proposito`() = runTest {
        coEvery { repo.plantas() } throws RuntimeException("sin conexion")

        val viewModel = crearViewModel()

        val estado = viewModel.state.value
        assertNull(estado.error)
        assertEquals(CONFIG_POR_DEFECTO.planta, estado.planta)
    }

    @Test
    fun `limpiarError borra el error sin tocar el resto del estado`() = runTest {
        coEvery { repo.puestos(any()) } throws RuntimeException("sin conexion")
        val viewModel = crearViewModel()
        assertNotNull(viewModel.state.value.error)

        viewModel.limpiarError()

        val estado = viewModel.state.value
        assertNull(estado.error)
        assertEquals(CONFIG_POR_DEFECTO.serverUrl, estado.serverUrl)
    }

    @Test
    fun `cambiarPlanta actualiza la planta y reinicia el puesto seleccionado`() = runTest {
        val viewModel = crearViewModel(CONFIG_POR_DEFECTO.copy(puestoIndex = 1))

        viewModel.cambiarPlanta("suipacha")

        val estado = viewModel.state.value
        assertEquals("suipacha", estado.planta)
        assertEquals(0, estado.puestoIndex)
        coVerify(exactly = 2) { repo.puestos(any()) } // 1 al iniciar + 1 al cambiar de planta
    }

    @Test
    fun `cambiarTipo recarga puestos para el nuevo tipo reiniciando el indice`() = runTest {
        val viewModel = crearViewModel(CONFIG_POR_DEFECTO.copy(puestoIndex = 1))

        viewModel.cambiarTipo("CALEFON")

        val estado = viewModel.state.value
        assertEquals("CALEFON", estado.tipo)
        assertEquals(0, estado.puestoIndex)
        coVerify { repo.puestos("CALEFON") }
    }

    @Test
    fun `seleccionarPuesto solo actualiza el indice, no dispara ninguna carga extra`() = runTest {
        val viewModel = crearViewModel()

        viewModel.seleccionarPuesto(1)

        assertEquals(1, viewModel.state.value.puestoIndex)
        coVerify(exactly = 1) { repo.puestos(any()) } // solo la carga inicial
    }

    @Test
    fun `confirmar persiste la configuracion con los datos del puesto seleccionado`() = runTest {
        val viewModel = crearViewModel()
        viewModel.seleccionarPuesto(1) // "Pintura", puestocontrol_c = "2"

        viewModel.confirmar()

        coVerify {
            store.guardar(
                ConfiguracionPuesto(
                    inicializado = true,
                    serverUrl = CONFIG_POR_DEFECTO.serverUrl,
                    planta = CONFIG_POR_DEFECTO.planta,
                    tipo = CONFIG_POR_DEFECTO.tipo,
                    puestoIndex = 1,
                    puestoNombre = "Pintura",
                    puestoC = 2,
                )
            )
        }
        assertTrue(viewModel.state.value.guardado)
    }

    @Test
    fun `confirmar no hace nada si no hay ningun puesto seleccionado valido`() = runTest {
        coEvery { repo.puestos(any()) } returns emptyList()
        val viewModel = crearViewModel()

        viewModel.confirmar()

        coVerify(exactly = 0) { store.guardar(any()) }
        assertFalse(viewModel.state.value.guardado)
    }
}
