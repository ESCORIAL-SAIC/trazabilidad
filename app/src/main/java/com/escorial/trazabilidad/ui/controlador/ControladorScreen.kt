package com.escorial.trazabilidad.ui.controlador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.escorial.trazabilidad.domain.FlujoActual
import com.escorial.trazabilidad.ui.common.CampoPickeo
import com.escorial.trazabilidad.ui.common.TzScaffold
import com.escorial.trazabilidad.ui.common.parseColorRRGGBB
import com.escorial.trazabilidad.ui.common.rememberBeep
import com.escorial.trazabilidad.ui.navigation.Routes
import com.escorial.trazabilidad.ui.theme.EkartGreen
import com.escorial.trazabilidad.ui.theme.EkartRed

/** Pantalla Controlador (TabItemControlador): registra OK o deriva a Falla. */
@Composable
fun ControladorScreen(nav: NavController, vm: ControladorViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val r = state.resolver
    val beep = rememberBeep()
    // Barral precargado por el backend (puesto Fuga): solo informativo.
    val barralPrecargado = r?.campoBarral?.valor?.takeIf { it.isNotBlank() }
    var barral by remember { mutableStateOf(barralPrecargado ?: "") }

    fun volverAScan() {
        FlujoActual.limpiar()
        nav.navigate(Routes.SCAN) { popUpTo(Routes.SCAN) { inclusive = true } }
    }

    LaunchedEffect(state.terminado) {
        if (state.terminado) {
            beep(true)
            if (state.mensaje == null) volverAScan()
        }
    }
    LaunchedEffect(state.error) { if (state.error != null) beep(false) }
    LaunchedEffect(state.graficaOk) { if (state.graficaOk != null) beep(true) }
    LaunchedEffect(state.graficaRechazo) {
        if (state.graficaRechazo != null) { beep(false); barral = "" }
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
                val etiquetaCampo = campo.prompt ?: "Barral"
                when {
                    // Fuga: el barral ya viene asociado, se muestra como dato (sin teclado).
                    barralPrecargado != null -> Text(
                        "$etiquetaCampo: $barralPrecargado",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    // Control Final: la grafica ya validada se muestra en verde.
                    state.graficaOk != null -> Text(
                        "$etiquetaCampo: ${state.graficaOk} ✓",
                        style = MaterialTheme.typography.bodyLarge,
                        color = EkartGreen,
                    )
                    else -> CampoPickeo(
                        value = barral,
                        onValueChange = { barral = it },
                        label = etiquetaCampo,
                        autoFocus = true,
                        focusKey = state.intentoGrafica,
                        keyboardType = KeyboardType.Ascii,
                        onEnter = { codigo ->
                            when {
                                state.esControlFinal -> vm.validarGrafica(codigo)
                                state.esVinculacionBarral -> vm.registrarOk(codigo)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.validandoGrafica) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Verificando gráfica frontal…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.weight(1f))
            if (!state.esVinculacionBarral && state.graficaValidada) {
                Button(
                    onClick = { vm.registrarOk(barralPrecargado ?: state.graficaOk ?: barral) },
                    enabled = !state.guardando,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EkartGreen),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.guardando) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("OK / Aprobar")
                }
                Button(
                    onClick = { nav.navigate(Routes.REPARADOR) },
                    enabled = !state.guardando,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EkartRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("NOK / RECHAZAR") }
            }
        }
    }

    state.mensaje?.let { msg ->
        AlertDialog(
            onDismissRequest = { volverAScan() },
            confirmButton = { TextButton(onClick = { volverAScan() }) { Text("OK") } },
            title = { Text("Listo") }, text = { Text(msg) },
        )
    }
    state.graficaRechazo?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.limpiarRechazoGrafica() },
            confirmButton = { TextButton(onClick = { vm.limpiarRechazoGrafica() }) { Text("Reintentar") } },
            title = { Text("RECHAZADO POR GRÁFICA FRONTAL") }, text = { Text(msg) },
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
