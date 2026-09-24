package com.escorial.trazabilidad.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escorial.trazabilidad.BuildConfig
import com.escorial.trazabilidad.data.api.dto.LoginRequest
import com.escorial.trazabilidad.data.local.ConfiguracionStore
import com.escorial.trazabilidad.data.repo.TrazabilidadRepository
import com.escorial.trazabilidad.domain.Sesion
import com.escorial.trazabilidad.domain.SesionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val ok: Boolean = false,
    /** Version de la app (BuildConfig.VERSION_NAME), fija. */
    val appVersion: String = BuildConfig.VERSION_NAME,
    /** Version de la API verificada al configurar el servidor. Null si nunca se verifico. */
    val apiVersion: String? = null,
)

class LoginViewModel(
    private val store: ConfiguracionStore,
    private val repo: TrazabilidadRepository = TrazabilidadRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(apiVersion = store.apiVersion.first())
        }
    }

    /** Replica ButtonIngresarClick: operario 1 obligatorio, operario 2 opcional. */
    fun ingresar(usuario1: String, pass1: String, usuario2: String, pass2: String) {
        _state.value = _state.value.copy(cargando = true, error = null, ok = false)
        viewModelScope.launch {
            try {
                val resp = repo.login(LoginRequest(usuario1, pass1, usuario2, pass2))
                if (resp.valido && resp.empleado1 != null && resp.empleado2 != null) {
                    SesionActual.sesion = Sesion(resp.empleado1, resp.empleado2)
                    _state.value = _state.value.copy(cargando = false, ok = true)
                } else {
                    _state.value = _state.value.copy(
                        cargando = false,
                        error = resp.error ?: "Datos de acceso incorrectos. Vuelva a intentarlo.",
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    cargando = false,
                    error = "No se pudo establecer conexion con el servidor. Revise la red e intente nuevamente.",
                )
            }
        }
    }

    fun limpiarError() {
        _state.value = _state.value.copy(error = null)
    }
}
