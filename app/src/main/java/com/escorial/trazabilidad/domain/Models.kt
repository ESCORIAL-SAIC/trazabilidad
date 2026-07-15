package com.escorial.trazabilidad.domain

import com.escorial.trazabilidad.data.api.dto.EmpleadoDto

/** Configuracion local del puesto (antes SQLite Configuracion.db). */
data class ConfiguracionPuesto(
    val inicializado: Boolean = false, // true tras completar el setup inicial de servidor
    val serverUrl: String = "http://10.90.99.114:3000/",
    val planta: String = "25demayo",
    val tipo: String = "COCINA",
    val puestoIndex: Int = 0,
    val puestoNombre: String = "Reparador",
    val puestoC: Int = 0,
)

/** Sesion en memoria de los operarios logueados. */
data class Sesion(
    val empleado1: EmpleadoDto,
    val empleado2: EmpleadoDto,
)

object SesionActual {
    @Volatile
    var sesion: Sesion? = null
}

/** Tipos de producto seleccionables (igual que ComboBoxTipo del Delphi). */
val TIPOS_PRODUCTO = listOf(
    "COCINA",
    "TERMOTANQUE",
    "TERMOTANQUE GAS",
    "TERMOTANQUE GEISER",
    "CALEFON",
    "BARRAL",
)
