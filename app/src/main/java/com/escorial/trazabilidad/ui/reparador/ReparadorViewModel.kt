package com.escorial.trazabilidad.ui.reparador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escorial.trazabilidad.data.api.dto.*
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import com.escorial.trazabilidad.domain.FlujoActual
import com.escorial.trazabilidad.domain.SesionActual
import com.escorial.trazabilidad.ui.controlador.mensajeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Modo de la pantalla: FALLA = controlador registra Nivel1; REPARACION = Nivel2->Nivel3. */
enum class ModoReparador { FALLA, REPARACION }

data class ReparadorUiState(
    val modo: ModoReparador = ModoReparador.FALLA,
    val nivel1: List<FallaNivel1Dto> = emptyList(),
    val nivel2: List<FallaNivel2Dto> = emptyList(),
    val nivel3: List<FallaNivel3Dto> = emptyList(),
    val nivel2Sel: FallaNivel2Dto? = null,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    val terminado: Boolean = false,
)

class ReparadorViewModel(
    private val repo: TrazabilidadRepository = TrazabilidadRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(estadoInicial())
    val state: StateFlow<ReparadorUiState> = _state

    private fun estadoInicial(): ReparadorUiState {
        val r = FlujoActual.resolver
        val esReparacion = r?.registroFalla == true
        return ReparadorUiState(
            modo = if (esReparacion) ModoReparador.REPARACION else ModoReparador.FALLA,
            nivel1 = r?.nivel1 ?: emptyList(),
            nivel2 = r?.nivel2 ?: emptyList(),
        )
    }

    /** Selecciona un Nivel2 y carga sus Nivel3 (modo reparacion). */
    fun seleccionarNivel2(n2: FallaNivel2Dto) {
        _state.value = _state.value.copy(cargando = true, nivel2Sel = n2, nivel3 = emptyList())
        viewModelScope.launch {
            try {
                val lista = repo.nivel3(n2.nivel2_id)
                _state.value = _state.value.copy(cargando = false, nivel3 = lista)
            } catch (e: Exception) {
                _state.value = _state.value.copy(cargando = false, error = mensajeError(e))
            }
        }
    }

    fun volverANivel2() {
        _state.value = _state.value.copy(nivel2Sel = null, nivel3 = emptyList())
    }

    /** ButtonAceptarClick (controlador): registra falla Nivel1. */
    fun aceptarFalla(n1: FallaNivel1Dto) {
        val r = FlujoActual.resolver ?: return
        val puesto = r.puestoAsignado ?: return
        val sesion = SesionActual.sesion ?: return
        _state.value = _state.value.copy(guardando = true, error = null)
        viewModelScope.launch {
            try {
                repo.controlFalla(
                    ControlFallaRequest(
                        etiqueta = r.etiqueta?.numero ?: 0,
                        puesto = PuestoRef(puesto.id, puesto.nombre, puesto.c),
                        controlador = EmpleadoRef(sesion.empleado1.id ?: "", sesion.empleado1.nombre ?: ""),
                        secundario = EmpleadoRef(sesion.empleado2.id ?: "", sesion.empleado2.nombre ?: ""),
                        nivel1 = NivelRef(n1.nivel1_id, n1.nivel1),
                        barral = r.campoBarral?.valor?.ifBlank { null },
                    ),
                )
                _state.value = _state.value.copy(guardando = false, terminado = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(guardando = false, error = mensajeError(e))
            }
        }
    }

    /** ButtonAceptarClick (reparador): registra reparacion Nivel3. */
    fun aceptarReparacion(n3: FallaNivel3Dto) {
        val r = FlujoActual.resolver ?: return
        val sesion = SesionActual.sesion ?: return
        val registroId = r.registroId ?: return
        _state.value = _state.value.copy(guardando = true, error = null)
        viewModelScope.launch {
            try {
                val resp = repo.controlReparacion(
                    ControlReparacionRequest(
                        registroId = registroId,
                        etiqueta = r.etiqueta?.numero ?: 0,
                        tipoProducto = FlujoActual.tipoProducto,
                        puestoNombre = r.puestoAsignado?.nombre ?: "",
                        reparador = EmpleadoRef(sesion.empleado1.id ?: "", sesion.empleado1.nombre ?: ""),
                        nivel3 = NivelRef(n3.nivel3_id, n3.nivel3),
                    ),
                )
                _state.value = _state.value.copy(guardando = false, terminado = true, mensaje = resp.mensaje)
            } catch (e: Exception) {
                _state.value = _state.value.copy(guardando = false, error = mensajeError(e))
            }
        }
    }

    fun limpiarError() { _state.value = _state.value.copy(error = null) }
}
