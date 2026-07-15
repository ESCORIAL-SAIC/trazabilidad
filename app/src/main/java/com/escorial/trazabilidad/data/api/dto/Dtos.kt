package com.escorial.trazabilidad.data.api.dto

import kotlinx.serialization.Serializable

/** Empleado devuelto por /auth/login (campos de VP_APLICACIONES_EMPLEADO). */
@Serializable
data class EmpleadoDto(
    val id: String? = null,
    val nombre: String? = null,
    val usuario: String? = null,
    val planta: String? = null,
)

@Serializable
data class LoginRequest(
    val usuario1: String,
    val password1: String,
    val usuario2: String = "",
    val password2: String = "",
)

@Serializable
data class LoginResponse(
    val valido: Boolean = false,
    val error: String? = null,
    val empleado1: EmpleadoDto? = null,
    val empleado2: EmpleadoDto? = null,
)

/** Puesto de control (VP_MENUFALLAS_PUESTOCONTROL_V1). puestocontrol_c es varchar. */
@Serializable
data class PuestoDto(
    val puestocontrol_id: String,
    val puestocontrol_n: String,
    val puestocontrol_c: String,
)

@Serializable
data class FallaNivel1Dto(
    val puestocontrol_id: String? = null,
    val puestocontrol_n: String? = null,
    val nivel1: String,
    val nivel1_id: String,
)

@Serializable
data class FallaNivel2Dto(
    val nivel1: String? = null,
    val nivel1_id: String? = null,
    val nivel2: String,
    val nivel2_id: String,
)

@Serializable
data class FallaNivel3Dto(
    val nivel2: String? = null,
    val nivel2_id: String? = null,
    val nivel3: String,
    val nivel3_id: String,
)

// ---- Resolver (POST /scan/resolver) ----
@Serializable
data class ResolverRequest(
    val numero: Int,
    val tipoProducto: String,
    val tipoConfig: String,
    val puestoConfigIndex: Int,
    val puestoConfigNombre: String,
    val puestoConfigC: Int,
)

@Serializable
data class PuestoAsignadoDto(val id: String, val nombre: String, val c: Int)

@Serializable
data class CampoBarralDto(
    val visible: Boolean = false,
    val readOnly: Boolean = false,
    val conCamara: Boolean = false,
    val prompt: String? = null,
    val valor: String? = null,
)

@Serializable
data class EstadoUiDto(val texto: String, val color: String)

@Serializable
data class EtiquetaDto(
    val numero: Int? = null,
    val producto_id: String? = null,
    val producto_n: String? = null,
    val color: String? = null,
    val tipo: String? = null,
)

@Serializable
data class ResolverResponse(
    val accion: String,
    val mensaje: String? = null,
    val color: String? = null,
    val etiqueta: EtiquetaDto? = null,
    val descripcion: String? = null,
    val puestoAsignado: PuestoAsignadoDto? = null,
    val registroFalla: Boolean? = null,
    val registroId: String? = null,
    val controladorFallaId: String? = null,
    val campoBarral: CampoBarralDto? = null,
    val estado: EstadoUiDto? = null,
    val nivel1: List<FallaNivel1Dto>? = null,
    val nivel2: List<FallaNivel2Dto>? = null,
)

// ---- Control (escritura) ----
@Serializable
data class EmpleadoRef(val id: String, val nombre: String)

@Serializable
data class PuestoRef(val id: String, val nombre: String, val c: Int)

@Serializable
data class NivelRef(val id: String, val nombre: String)

@Serializable
data class ControlOkRequest(
    val etiqueta: Int,
    val tipoProducto: String,
    val productoId: String,
    val puesto: PuestoRef,
    val controlador: EmpleadoRef,
    val secundario: EmpleadoRef,
    val barral: String? = null,
)

@Serializable
data class ControlOkResponse(
    val id: String,
    val liberado: Boolean = false,
    val mensaje: String? = null,
)

@Serializable
data class ControlFallaRequest(
    val etiqueta: Int,
    val puesto: PuestoRef,
    val controlador: EmpleadoRef,
    val secundario: EmpleadoRef,
    val nivel1: NivelRef,
    val barral: String? = null,
)

@Serializable
data class ControlReparacionRequest(
    val registroId: String,
    val etiqueta: Int,
    val tipoProducto: String,
    val puestoNombre: String,
    val reparador: EmpleadoRef,
    val nivel3: NivelRef,
)

@Serializable
data class MensajeResponse(val mensaje: String? = null, val id: String? = null)

@Serializable
data class PlantaDto(val id: String, val nombre: String)
