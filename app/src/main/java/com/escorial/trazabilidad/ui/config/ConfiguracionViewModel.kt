package com.escorial.trazabilidad.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escorial.trazabilidad.data.api.ApiConfig
import com.escorial.trazabilidad.data.api.dto.PlantaDto
import com.escorial.trazabilidad.data.api.dto.PuestoDto
import com.escorial.trazabilidad.data.local.ConfiguracionStore
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import com.escorial.trazabilidad.BuildConfig
import com.escorial.trazabilidad.domain.ConfiguracionPuesto
import com.escorial.trazabilidad.ui.common.VERSION_DESCONOCIDA
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ConfigUiState(
    val serverUrl: String = "http://10.90.99.114:3000/",
    val planta: String = "25demayo",
    val plantas: List<PlantaDto> = emptyList(),
    val tipo: String = "COCINA",
    val puestos: List<PuestoDto> = emptyList(),
    val puestoIndex: Int = 0,
    val cargando: Boolean = false,
    val error: String? = null,
    val guardado: Boolean = false,
    /** Version de la app (BuildConfig.VERSION_NAME), fija. */
    val appVersion: String = BuildConfig.VERSION_NAME,
    /** Version de la API verificada al conectar. Null si nunca se verifico. */
    val apiVersion: String? = null,
)

class ConfiguracionViewModel(
    private val store: ConfiguracionStore,
    private val repo: TrazabilidadRepository = TrazabilidadRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(ConfigUiState())
    val state: StateFlow<ConfigUiState> = _state

    init {
        viewModelScope.launch {
            val cfg = store.configuracion.first()
            _state.value = _state.value.copy(
                serverUrl = cfg.serverUrl,
                planta = cfg.planta,
                tipo = cfg.tipo,
                puestoIndex = cfg.puestoIndex,
                apiVersion = store.apiVersion.first(),
            )
            ApiConfig.baseUrl = cfg.serverUrl
            ApiConfig.planta = cfg.planta
            cargarPlantas()
            cargarPuestos(cfg.tipo, cfg.puestoIndex)
        }
    }

    fun cambiarServerUrl(url: String) { _state.value = _state.value.copy(serverUrl = url) }

    /** Selección de planta desde el desplegable: actualiza ApiConfig y recarga puestos. */
    fun cambiarPlanta(id: String) {
        _state.value = _state.value.copy(planta = id)
        ApiConfig.planta = id
        cargarPuestos(_state.value.tipo, 0)
    }

    fun cambiarTipo(tipo: String) {
        _state.value = _state.value.copy(tipo = tipo)
        cargarPuestos(tipo, 0)
    }

    fun seleccionarPuesto(index: Int) { _state.value = _state.value.copy(puestoIndex = index) }

    /** Aplica URL/planta y recarga plantas + puestos desde la API. */
    fun recargar() {
        ApiConfig.baseUrl = _state.value.serverUrl
        ApiConfig.planta = _state.value.planta
        cargarPlantas()
        cargarPuestos(_state.value.tipo, _state.value.puestoIndex)
    }

    private fun cargarPlantas() {
        viewModelScope.launch {
            try {
                val lista = repo.plantas()
                val plantaSel = if (lista.any { it.id == _state.value.planta }) _state.value.planta
                else lista.firstOrNull()?.id ?: _state.value.planta
                _state.value = _state.value.copy(plantas = lista, planta = plantaSel)
                ApiConfig.planta = plantaSel
                leerVersionApi()
            } catch (e: Exception) {
                // Fallo intencionalmente silencioso: si el servidor no puede listar plantas,
                // se deja la planta configurada previamente como única opción y no se bloquea
                // la pantalla con un error, porque cargarPuestos() puede seguir funcionando
                // igual (mismo servidor, misma planta ya persistida) y ese sí es bloqueante.
            }
        }
    }

    /**
     * Version de la API, informativa: si /version falla no se invalida la conexion
     * (el servidor puede ser viejo y no tener el endpoint), solo se marca desconocida.
     */
    private suspend fun leerVersionApi() {
        val version = try {
            repo.version().version?.takeIf { it.isNotBlank() } ?: VERSION_DESCONOCIDA
        } catch (e: Exception) {
            VERSION_DESCONOCIDA
        }
        _state.value = _state.value.copy(apiVersion = version)
        store.guardarApiVersion(version)
    }

    private fun cargarPuestos(tipo: String, index: Int) {
        ApiConfig.baseUrl = _state.value.serverUrl
        ApiConfig.planta = _state.value.planta
        _state.value = _state.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            try {
                val lista = repo.puestos(tipo)
                val idx = if (index < lista.size) index else 0
                _state.value = _state.value.copy(puestos = lista, puestoIndex = idx, cargando = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(cargando = false, error = "No se pudieron cargar los puestos. Verifique la URL del servidor.")
            }
        }
    }

    /** ButtonConfirmarClick: persiste todo y actualiza ApiConfig. */
    fun confirmar() {
        val s = _state.value
        val puesto = s.puestos.getOrNull(s.puestoIndex) ?: return
        viewModelScope.launch {
            store.guardar(
                ConfiguracionPuesto(
                    inicializado = true,
                    serverUrl = s.serverUrl,
                    planta = s.planta,
                    tipo = s.tipo,
                    puestoIndex = s.puestoIndex,
                    puestoNombre = puesto.puestocontrol_n,
                    puestoC = puesto.puestocontrol_c.toIntOrNull() ?: 0,
                )
            )
            ApiConfig.baseUrl = s.serverUrl
            ApiConfig.planta = s.planta
            _state.value = _state.value.copy(guardado = true)
        }
    }

    fun limpiarError() { _state.value = _state.value.copy(error = null) }
}
