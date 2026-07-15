package com.escorial.trazabilidad.data.api

/**
 * Configuracion de red vigente (URL del servidor + planta). Se inicializa al
 * arrancar la app desde DataStore y se actualiza al confirmar Configuracion.
 * El interceptor de OkHttp lee [planta] para enviar el header X-Planta.
 */
object ApiConfig {
    @Volatile
    var baseUrl: String = "http://10.90.99.114:3000/"

    @Volatile
    var planta: String = "25demayo"
}
