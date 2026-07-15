package com.escorial.trazabilidad.ui.estado

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.escorial.trazabilidad.domain.FlujoActual
import com.escorial.trazabilidad.ui.common.TzScaffold
import com.escorial.trazabilidad.ui.navigation.Routes
import com.escorial.trazabilidad.ui.theme.EkartGreen
import com.escorial.trazabilidad.ui.theme.EkartRed

/** Pantalla Estado (TabItemEstado): semáforo del producto. */
@Composable
fun EstadoScreen(nav: NavController) {
    val r = FlujoActual.resolver
    val estado = r?.estado

    fun volver() {
        FlujoActual.limpiar()
        nav.navigate(Routes.SCAN) { popUpTo(Routes.SCAN) { inclusive = true } }
    }

    TzScaffold(titulo = "Estado", onBack = { volver() }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            r?.descripcion?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }
            Text("Etiqueta: ${r?.etiqueta?.numero ?: "-"}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            val color: Color = if (estado?.color == "verde") EkartGreen else EkartRed
            Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(color))
            Spacer(Modifier.height(24.dp))
            Text(estado?.texto ?: "Sin información", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { volver() }, modifier = Modifier.fillMaxWidth()) { Text("Aceptar") }
        }
    }
}
