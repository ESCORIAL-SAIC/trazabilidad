package com.escorial.trazabilidad.ui.common

/** Valor guardado cuando el servidor respondio pero no se pudo leer su version. */
const val VERSION_DESCONOCIDA = "desconocida"

/**
 * Etiqueta legible de la version de la API.
 *
 * El prefijo "v" se agrega solo a versiones que parecen un numero: `GET /version` devuelve
 * "0.0.0-dev" o similar segun APP_VERSION, y para valores no numericos "API vdev" se leeria mal.
 */
fun apiVersionLabel(apiVersion: String?): String = when {
    apiVersion.isNullOrBlank() -> "API sin verificar"
    apiVersion == VERSION_DESCONOCIDA -> "API (versión desconocida)"
    apiVersion.first().isDigit() -> "API v$apiVersion"
    else -> "API $apiVersion"
}

/**
 * Pie de versiones compartido por Login y Configuracion, para que ambas pantallas digan lo mismo.
 *
 * [apiVersion] es la que reporto `GET /version` la ultima vez que se verifico la conexion
 * (persistida en ConfiguracionStore); es null si nunca se verifico, y puede quedar
 * desactualizada si el servidor se actualiza sin reconfigurar la app.
 */
fun versionesTexto(appVersion: String, apiVersion: String?): String =
    "App v$appVersion · ${apiVersionLabel(apiVersion)}"
