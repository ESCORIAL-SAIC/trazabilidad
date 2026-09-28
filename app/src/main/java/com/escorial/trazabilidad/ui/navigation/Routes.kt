package com.escorial.trazabilidad.ui.navigation

object Routes {
    const val SETUP = "setup"        // configuracion de servidor previa al login
    const val LOGIN = "login"
    const val SCAN = "scan"          // pantalla de entrada/escaneo (TabItemBlanco)
    const val CONFIG_API = "config_api"        // TabItemConfiguracion: URL de la API (pide clave)
    const val CONFIG_PUESTO = "config_puesto"  // TabItemConfiguracion: planta + puesto (sin clave)
    const val CONTROLADOR = "controlador"
    const val REPARADOR = "reparador"
    const val ESTADO = "estado"
}
