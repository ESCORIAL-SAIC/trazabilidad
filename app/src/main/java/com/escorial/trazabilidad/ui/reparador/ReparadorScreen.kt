package com.escorial.trazabilidad.ui.reparador

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.escorial.trazabilidad.domain.FlujoActual
import com.escorial.trazabilidad.ui.common.TzScaffold
import com.escorial.trazabilidad.ui.navigation.Routes

/** Pantalla Reparador: Nivel1 (falla) o Nivel2->Nivel3 (reparación). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReparadorScreen(nav: NavController, vm: ReparadorViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    fun volverAScan() {
        FlujoActual.limpiar()
        nav.navigate(Routes.SCAN) { popUpTo(Routes.SCAN) { inclusive = true } }
    }

    fun atras() {
        if (state.modo == ModoReparador.REPARACION && state.nivel2Sel != null) vm.volverANivel2()
        else volverAScan()
    }

    LaunchedEffect(state.terminado) {
        if (state.terminado && state.mensaje == null) volverAScan()
    }

    val titulo = when (state.modo) {
        ModoReparador.FALLA -> "Falla (Nivel 1)"
        ModoReparador.REPARACION -> if (state.nivel2Sel == null) "Reparación — Nivel 2" else "Reparación — Nivel 3"
    }

    TzScaffold(titulo = titulo, onBack = { atras() }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state.guardando || state.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())

            Box(Modifier.weight(1f)) {
                when (state.modo) {
                    ModoReparador.FALLA -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(state.nivel1) { n1 ->
                            ListItem(
                                headlineContent = { Text(n1.nivel1) },
                                modifier = Modifier.clickable(enabled = !state.guardando) { vm.aceptarFalla(n1) },
                            )
                            HorizontalDivider()
                        }
                    }
                    ModoReparador.REPARACION -> {
                        if (state.nivel2Sel == null) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.nivel2) { n2 ->
                                    ListItem(
                                        headlineContent = { Text(n2.nivel2) },
                                        modifier = Modifier.clickable { vm.seleccionarNivel2(n2) },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.nivel3) { n3 ->
                                    ListItem(
                                        headlineContent = { Text(n3.nivel3) },
                                        modifier = Modifier.clickable(enabled = !state.guardando) { vm.aceptarReparacion(n3) },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.mensaje?.let { msg ->
        AlertDialog(
            onDismissRequest = { volverAScan() },
            confirmButton = { TextButton(onClick = { volverAScan() }) { Text("OK") } },
            title = { Text("Atención") }, text = { Text(msg) },
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
