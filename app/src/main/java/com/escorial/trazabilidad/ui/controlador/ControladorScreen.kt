package com.escorial.trazabilidad.ui.controlador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.escorial.trazabilidad.domain.FlujoActual
import com.escorial.trazabilidad.ui.common.CampoPickeo
import com.escorial.trazabilidad.ui.common.TzScaffold
import com.escorial.trazabilidad.ui.common.parseColorRRGGBB
import com.escorial.trazabilidad.ui.common.rememberEscaner
import com.escorial.trazabilidad.ui.navigation.Routes
import com.escorial.trazabilidad.ui.theme.EkartGreen

/** Pantalla Controlador (TabItemControlador): registra OK o deriva a Falla. */
@Composable
fun ControladorScreen(nav: NavController, vm: ControladorViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val r = state.resolver
    var barral by remember { mutableStateOf(r?.campoBarral?.valor ?: "") }
    val escanear = rememberEscaner { codigo -> barral = codigo }

    fun volverAScan() {
        FlujoActual.limpiar()
        nav.navigate(Routes.SCAN) { popUpTo(Routes.SCAN) { inclusive = true } }
    }

    LaunchedEffect(state.terminado) {
        if (state.terminado && state.mensaje == null) volverAScan()
    }

    if (r == null) {
        LaunchedEffect(Unit) { volverAScan() }
        return
    }

    val fondo = parseColorRRGGBB(r.color)
    TzScaffold(titulo = "Control", onBack = { volverAScan() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(fondo.copy(alpha = 0.12f))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(r.descripcion ?: "Producto", style = MaterialTheme.typography.headlineSmall)
            Text("Etiqueta: ${r.etiqueta?.numero ?: "-"}", style = MaterialTheme.typography.bodyLarge)
            Text("Puesto: ${r.puestoAsignado?.nombre ?: "-"}", style = MaterialTheme.typography.bodyMedium)

            val campo = r.campoBarral
            if (campo?.visible == true) {
                CampoPickeo(
                    value = barral,
                    onValueChange = { barral = it },
                    label = campo.prompt ?: "Barral",
                    autoFocus = true,
                    keyboardType = KeyboardType.Ascii,
                    // Enter de la pistola: solo saca el foco (NO confirma).
                    onEnter = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                if (campo.conCamara) {
                    OutlinedButton(onClick = escanear, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Escanear código frontal")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { vm.registrarOk(barral) },
                enabled = !state.guardando,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EkartGreen),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.guardando) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("OK / Aprobar")
            }
            OutlinedButton(
                onClick = { nav.navigate(Routes.REPARADOR) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Registrar Falla") }
        }
    }

    state.mensaje?.let { msg ->
        AlertDialog(
            onDismissRequest = { volverAScan() },
            confirmButton = { TextButton(onClick = { volverAScan() }) { Text("OK") } },
            title = { Text("Listo") }, text = { Text(msg) },
        )
    }
    state.error?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.limpiarError() },
            confirmButton = { TextButton(onClick = { vm.limpiarError() }) { Text("OK") } },
            title = { Text("Error") }, text = { Text(msg) },
        )
    }
}
