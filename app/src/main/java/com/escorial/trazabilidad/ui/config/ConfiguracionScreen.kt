package com.escorial.trazabilidad.ui.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.escorial.trazabilidad.data.local.ConfiguracionStore
import com.escorial.trazabilidad.domain.TIPOS_PRODUCTO
import com.escorial.trazabilidad.ui.common.TzScaffold
import com.escorial.trazabilidad.ui.common.versionesTexto
import com.escorial.trazabilidad.ui.navigation.Routes

/**
 * Configuración de la API: solo la URL del servidor. Va detrás de la clave
 * (ver [com.escorial.trazabilidad.ui.common.DialogoClaveConfig]) porque apunta la app
 * a otro servidor y no es algo que deba tocar el operario.
 */
@Composable
fun ConfiguracionApiScreen(nav: NavController, vm: ConfiguracionViewModel = configuracionViewModel()) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.guardado) {
        if (state.guardado) {
            nav.navigate(Routes.SCAN) { popUpTo(Routes.SCAN) { inclusive = true } }
        }
    }

    TzScaffold(titulo = "Configuración de API", onBack = { nav.popBackStack() }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Servidor (URL de la API)", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = state.serverUrl, onValueChange = { vm.cambiarServerUrl(it) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("http://ip:3000/") },
            )

            OutlinedButton(onClick = { vm.recargar() }, modifier = Modifier.fillMaxWidth()) {
                Text("Conectar / recargar")
            }

            Button(
                onClick = { vm.guardarApi() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Confirmar") }

            Text(
                text = versionesTexto(appVersion = state.appVersion, apiVersion = state.apiVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    DialogoError(state.error, vm::limpiarError)
}

/**
 * Configuración del puesto: planta, tipo de producto y puesto de control.
 * Sin clave: es lo que el operario cambia al arrancar el turno en otra línea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionPuestoScreen(nav: NavController, vm: ConfiguracionViewModel = configuracionViewModel()) {
    val state by vm.state.collectAsState()
    var tipoExpanded by remember { mutableStateOf(false) }
    var plantaExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.guardado) {
        if (state.guardado) {
            nav.navigate(Routes.SCAN) { popUpTo(Routes.SCAN) { inclusive = true } }
        }
    }

    TzScaffold(titulo = "Configuración de puesto", onBack = { nav.popBackStack() }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Planta", style = MaterialTheme.typography.labelLarge)
            val plantaSel = state.plantas.firstOrNull { it.id == state.planta }
            ExposedDropdownMenuBox(expanded = plantaExpanded, onExpandedChange = { plantaExpanded = it }) {
                OutlinedTextField(
                    value = plantaSel?.nombre ?: state.planta,
                    onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plantaExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = plantaExpanded, onDismissRequest = { plantaExpanded = false }) {
                    if (state.plantas.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sin plantas (revise el servidor)") },
                            onClick = { plantaExpanded = false },
                        )
                    } else {
                        state.plantas.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.nombre) },
                                onClick = { vm.cambiarPlanta(p.id); plantaExpanded = false },
                            )
                        }
                    }
                }
            }

            Text("Tipo de producto", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(expanded = tipoExpanded, onExpandedChange = { tipoExpanded = it }) {
                OutlinedTextField(
                    value = state.tipo, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = tipoExpanded, onDismissRequest = { tipoExpanded = false }) {
                    TIPOS_PRODUCTO.forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = { vm.cambiarTipo(t); tipoExpanded = false })
                    }
                }
            }

            Text("Puesto de control", style = MaterialTheme.typography.labelLarge)
            if (state.cargando) {
                CircularProgressIndicator()
            } else {
                Column {
                    state.puestos.forEachIndexed { index, p ->
                        Row(
                            Modifier.fillMaxWidth().selectable(
                                selected = index == state.puestoIndex,
                                onClick = { vm.seleccionarPuesto(index) },
                            ).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = index == state.puestoIndex, onClick = { vm.seleccionarPuesto(index) })
                            Spacer(Modifier.width(8.dp))
                            Text(p.puestocontrol_n)
                        }
                    }
                }
            }

            Button(
                onClick = { vm.confirmar() },
                enabled = state.puestos.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Confirmar") }
        }
    }

    DialogoError(state.error, vm::limpiarError)
}

@Composable
private fun DialogoError(mensaje: String?, onCerrar: () -> Unit) {
    mensaje?.let { msg ->
        AlertDialog(
            onDismissRequest = onCerrar,
            confirmButton = { TextButton(onClick = onCerrar) { Text("OK") } },
            title = { Text("Error") }, text = { Text(msg) },
        )
    }
}

/** Construye el ViewModel inyectando un [ConfiguracionStore] atado al contexto de la app. */
@Composable
private fun configuracionViewModel(): ConfiguracionViewModel {
    val context = LocalContext.current
    val factory = remember(context) {
        viewModelFactory {
            initializer { ConfiguracionViewModel(store = ConfiguracionStore(context.applicationContext)) }
        }
    }
    return viewModel(factory = factory)
}
