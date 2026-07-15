package com.escorial.trazabilidad.domain

import com.escorial.trazabilidad.data.api.dto.ResolverResponse

/** Resultado del ultimo /scan/resolver, compartido entre pantallas del flujo. */
object FlujoActual {
    @Volatile
    var resolver: ResolverResponse? = null

    @Volatile
    var tipoProducto: String = "COCINA"

    fun limpiar() {
        resolver = null
    }
}
