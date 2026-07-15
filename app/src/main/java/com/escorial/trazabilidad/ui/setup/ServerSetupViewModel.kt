package com.escorial.trazabilidad.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.escorial.trazabilidad.data.api.ApiConfig
import com.escorial.trazabilidad.data.api.dto.PlantaDto
import com.escorial.trazabilidad.data.local.ConfiguracionStore
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SetupUiState(
    val serverUrl: String = "http://10.90.99.114:3000/",
    val planta: String = "25demayo",
    val plantas: List<PlantaDto> = emptyList(),
    val probando: Boolean = false,
    val conectado: Boolean = false,
    val error: String? = null,
    val guardado: Boolean = false,
)

class ServerSetupViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TrazabilidadRepository()
    private val store = ConfiguracionStore(app)

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state

    init {
        viewModelScope.launch {
            val cfg = store.configuracion.first()
            _state.value = _state.value.copy(serverUrl = cfg.serverUrl, planta = cfg.planta)
        }
    }

    fun cambiarServerUrl(url: String) {
        _state.value = _state.value.copy(serverUrl = url, conectado = false)
    }

    fun cambiarPlanta(p: String) { _state.value = _state.value.copy(planta = p) }

    /** Aplica la URL y prueba la conexion pidiendo la lista de plantas. */
    fun probarConexion() {
        ApiConfig.baseUrl = _state.value.serverUrl
        _state.value = _state.value.copy(probando = true, error = null, conectado = false)
        viewModelScope.launch {
            try {
                val lista = repo.plantas()
                val planta = if (lista.any { it.id == _state.value.planta }) _state.value.planta
                else lista.firstOrNull()?.id ?: _state.value.planta
                _state.value = _state.value.copy(
                    plantas = lista, planta = planta, probando = false, conectado = true,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    probando = false,
                    error = "No se pudo conectar al servidor. Verifique la URL y la red.",
                )
            }
        }
    }

    /** Guarda servidor + planta, marca inicializado y avanza al login. */
    fun continuar() {
        viewModelScope.launch {
            store.guardarServidor(_state.value.serverUrl, _state.value.planta)
            ApiConfig.baseUrl = _state.value.serverUrl
            ApiConfig.planta = _state.value.planta
            _state.value = _state.value.copy(guardado = true)
        }
    }

    fun limpiarError() { _state.value = _state.value.copy(error = null) }
}
