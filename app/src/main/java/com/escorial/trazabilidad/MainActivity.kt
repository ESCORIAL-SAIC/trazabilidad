package com.escorial.trazabilidad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.escorial.trazabilidad.data.api.ApiConfig
import com.escorial.trazabilidad.data.local.ConfiguracionStore
import com.escorial.trazabilidad.ui.navigation.NavGraph
import com.escorial.trazabilidad.ui.navigation.Routes
import com.escorial.trazabilidad.ui.theme.TrazabilidadTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrazabilidadTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var start by remember { mutableStateOf<String?>(null) }
                    // Carga la config guardada; primer arranque => SETUP, si no => LOGIN.
                    LaunchedEffect(Unit) {
                        val cfg = ConfiguracionStore(applicationContext).configuracion.first()
                        ApiConfig.baseUrl = cfg.serverUrl
                        ApiConfig.planta = cfg.planta
                        start = if (cfg.inicializado) Routes.LOGIN else Routes.SETUP
                    }
                    val destino = start
                    if (destino != null) {
                        NavGraph(startDestination = destino)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
