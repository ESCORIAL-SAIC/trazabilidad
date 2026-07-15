package com.escorial.trazabilidad.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.escorial.trazabilidad.data.api.dto.ResolverRequest
import com.escorial.trazabilidad.data.local.ConfiguracionStore
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import com.escorial.trazabilidad.domain.ConfiguracionPuesto
import com.escorial.trazabilidad.domain.FlujoActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ScanUiState(
    val config: ConfiguracionPuesto = ConfiguracionPuesto(),
    val cargando: Boolean = false,
    val error: String? = null,
    val navegarA: String? = null, // accion del resolver (CONTROLADOR/REPARADOR/ESTADO)
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TrazabilidadRepository()
    private val store = ConfiguracionStore(app)
    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(config = store.configuracion.first())
        }
    }

    /** Replica SearchEditButton1Click: envia la etiqueta al resolver y decide pantalla. */
    fun buscar(numeroTexto: String) {
        val numero = numeroTexto.trim().toIntOrNull()
        if (numero == null) {
            _state.value = _state.value.copy(error = "Ingrese un número de etiqueta válido.")
            return
        }
        val cfg = _state.value.config
        _state.value = _state.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            try {
                val resp = repo.resolver(
                    ResolverRequest(
                        numero = numero,
                        tipoProducto = cfg.tipo,
                        tipoConfig = cfg.tipo,
                        puestoConfigIndex = cfg.puestoIndex,
                        puestoConfigNombre = cfg.puestoNombre,
                        puestoConfigC = cfg.puestoC,
                    ),
                )
                when (resp.accion) {
                    "IGNORAR" -> _state.value = _state.value.copy(cargando = false)
                    "ETIQUETA_INVALIDA" -> _state.value = _state.value.copy(
                        cargando = false,
                        error = resp.mensaje ?: "Etiqueta no válida.",
                    )
                    else -> {
                        FlujoActual.resolver = resp
                        FlujoActual.tipoProducto = cfg.tipo
                        _state.value = _state.value.copy(cargando = false, navegarA = resp.accion)
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(cargando = false, error = "Error de conexión con el servidor.")
            }
        }
    }

    fun navegacionConsumida() { _state.value = _state.value.copy(navegarA = null) }
    fun limpiarError() { _state.value = _state.value.copy(error = null) }
}
