package com.escorial.trazabilidad.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.escorial.trazabilidad.ui.config.ConfiguracionScreen
import com.escorial.trazabilidad.ui.controlador.ControladorScreen
import com.escorial.trazabilidad.ui.estado.EstadoScreen
import com.escorial.trazabilidad.ui.login.LoginScreen
import com.escorial.trazabilidad.ui.reparador.ReparadorScreen
import com.escorial.trazabilidad.ui.scan.ScanScreen
import com.escorial.trazabilidad.ui.setup.ServerSetupScreen

@Composable
fun NavGraph(startDestination: String) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.SETUP) { ServerSetupScreen(nav) }
        composable(Routes.LOGIN) { LoginScreen(nav) }
        composable(Routes.SCAN) { ScanScreen(nav) }
        composable(Routes.CONFIG) { ConfiguracionScreen(nav) }
        composable(Routes.CONTROLADOR) { ControladorScreen(nav) }
        composable(Routes.REPARADOR) { ReparadorScreen(nav) }
        composable(Routes.ESTADO) { EstadoScreen(nav) }
    }
}
