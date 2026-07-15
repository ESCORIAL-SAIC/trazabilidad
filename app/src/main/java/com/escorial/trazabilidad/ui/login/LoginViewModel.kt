package com.escorial.trazabilidad.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escorial.trazabilidad.data.api.dto.LoginRequest
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import com.escorial.trazabilidad.domain.Sesion
import com.escorial.trazabilidad.domain.SesionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val ok: Boolean = false,
)

class LoginViewModel(
    private val repo: TrazabilidadRepository = TrazabilidadRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    /** Replica ButtonIngresarClick: operario 1 obligatorio, operario 2 opcional. */
    fun ingresar(usuario1: String, pass1: String, usuario2: String, pass2: String) {
        _state.value = LoginUiState(cargando = true)
        viewModelScope.launch {
            try {
                val resp = repo.login(LoginRequest(usuario1, pass1, usuario2, pass2))
                if (resp.valido && resp.empleado1 != null && resp.empleado2 != null) {
                    SesionActual.sesion = Sesion(resp.empleado1, resp.empleado2)
                    _state.value = LoginUiState(ok = true)
                } else {
                    _state.value = LoginUiState(
                        error = resp.error ?: "Datos de acceso incorrectos. Vuelva a intentarlo.",
                    )
                }
            } catch (e: Exception) {
                _state.value = LoginUiState(
                    error = "No se pudo establecer conexion con el servidor. Revise la red e intente nuevamente.",
                )
            }
        }
    }

    fun limpiarError() {
        _state.value = _state.value.copy(error = null)
    }
}
