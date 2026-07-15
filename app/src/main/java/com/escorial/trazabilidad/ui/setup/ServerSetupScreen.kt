package com.escorial.trazabilidad.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.escorial.trazabilidad.ui.navigation.Routes

/**
 * Configuracion de servidor previa al login (primer arranque).
 * Carga la URL de la API + planta, prueba la conexion y avanza al login.
 */
@Composable
fun ServerSetupScreen(nav: NavController, vm: ServerSetupViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.guardado) {
        if (state.guardado) {
            nav.navigate(Routes.LOGIN) { popUpTo(Routes.SETUP) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Configuración del servidor", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Indicá la dirección de la API y la planta antes de ingresar.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = { vm.cambiarServerUrl(it) },
            label = { Text("URL del servidor") },
            placeholder = { Text("http://ip:3000/") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { vm.probarConexion() },
            enabled = !state.probando && state.serverUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.probando) CircularProgressIndicator(Modifier.size(20.dp))
            else Text("Probar conexión")
        }

        if (state.conectado) {
            Text("Planta", style = MaterialTheme.typography.labelLarge)
            if (state.plantas.isEmpty()) {
                Text("El servidor no devolvió plantas.", style = MaterialTheme.typography.bodySmall)
            } else {
                Column {
                    state.plantas.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().selectable(
                                selected = p.id == state.planta,
                                onClick = { vm.cambiarPlanta(p.id) },
                            ).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = p.id == state.planta, onClick = { vm.cambiarPlanta(p.id) })
                            Spacer(Modifier.width(8.dp))
                            Text("${p.nombre}  (${p.id})")
                        }
                    }
                }
            }

            Button(onClick = { vm.continuar() }, modifier = Modifier.fillMaxWidth()) {
                Text("Continuar al ingreso")
            }
        }
    }

    state.error?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.limpiarError() },
            confirmButton = { TextButton(onClick = { vm.limpiarError() }) { Text("OK") } },
            title = { Text("Error de conexión") },
            text = { Text(msg) },
        )
    }
}
