package com.escorial.trazabilidad.ui.controlador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escorial.trazabilidad.data.api.dto.*
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import com.escorial.trazabilidad.domain.FlujoActual
import com.escorial.trazabilidad.domain.PUESTO_CONTROL_FINAL
import com.escorial.trazabilidad.domain.SesionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ControladorUiState(
    val resolver: ResolverResponse? = FlujoActual.resolver,
    val guardando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null, // p.ej. "Producto LIBERADO."
    val terminado: Boolean = false,
    val esVinculacionBarral: Boolean = FlujoActual.tipoProducto == "BARRAL",
    // Control Final: hasta que la grafica frontal no coincida no se habilita OK/NOK.
    val esControlFinal: Boolean =
        FlujoActual.resolver?.puestoAsignado?.nombre == PUESTO_CONTROL_FINAL,
    val validandoGrafica: Boolean = false,
    val graficaValidada: Boolean =
        FlujoActual.resolver?.puestoAsignado?.nombre != PUESTO_CONTROL_FINAL,
    val graficaOk: String? = null,    // codigo frontal ya validado
    val graficaRechazo: String? = null, // motivo del rechazo (dialogo)
    val intentoGrafica: Int = 0,      // clave para reenfocar el campo tras un rechazo
)

class ControladorViewModel(
    private val repo: TrazabilidadRepository = TrazabilidadRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(ControladorUiState())
    val state: StateFlow<ControladorUiState> = _state

    /**
     * QueryCBFrontal: verifica que la grafica pickeada corresponda al producto.
     * Hasta que coincida no se muestran los botones de control final.
     */
    fun validarGrafica(codigo: String) {
        val productoId = _state.value.resolver?.etiqueta?.producto_id ?: return
        if (codigo.isBlank() || _state.value.validandoGrafica) return
        _state.value = _state.value.copy(validandoGrafica = true, error = null)
        viewModelScope.launch {
            try {
                repo.validarFrontal(ValidarFrontalRequest(codigo, productoId))
                _state.value = _state.value.copy(
                    validandoGrafica = false,
                    graficaValidada = true,
                    graficaOk = codigo,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    validandoGrafica = false,
                    graficaValidada = false,
                    graficaOk = null,
                    graficaRechazo = mensajeError(e),
                    intentoGrafica = _state.value.intentoGrafica + 1,
                )
            }
        }
    }

    /** ButtonOKClick: registra control OK. */
    fun registrarOk(barral: String?) {
        val r = _state.value.resolver ?: return
        val puesto = r.puestoAsignado ?: return
        val sesion = SesionActual.sesion ?: return
        if (_state.value.guardando) return
        val emp1 = sesion.empleado1
        val emp2 = sesion.empleado2
        _state.value = _state.value.copy(guardando = true, error = null)
        viewModelScope.launch {
            try {
                val resp = repo.controlOk(
                    ControlOkRequest(
                        etiqueta = r.etiqueta?.numero ?: 0,
                        tipoProducto = FlujoActual.tipoProducto,
                        productoId = r.etiqueta?.producto_id ?: "",
                        puesto = PuestoRef(puesto.id, puesto.nombre, puesto.c),
                        controlador = EmpleadoRef(emp1.id ?: "", emp1.nombre ?: ""),
                        secundario = EmpleadoRef(emp2.id ?: "", emp2.nombre ?: ""),
                        barral = barral?.ifBlank { null },
                    ),
                )
                _state.value = _state.value.copy(
                    guardando = false,
                    mensaje = resp.mensaje,
                    terminado = true,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    guardando = false,
                    error = mensajeError(e),
                )
            }
        }
    }

    fun limpiarError() { _state.value = _state.value.copy(error = null) }
    fun limpiarRechazoGrafica() { _state.value = _state.value.copy(graficaRechazo = null) }
}

/** Extrae el mensaje de negocio del cuerpo de error HTTP si existe. */
fun mensajeError(e: Exception): String {
    val http = e as? retrofit2.HttpException
    val body = http?.response()?.errorBody()?.string()
    if (body != null) {
        val m = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)
        if (m != null) return m.groupValues[1]
    }
    return "Error de conexión con el servidor."
}
