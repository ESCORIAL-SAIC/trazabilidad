package com.escorial.trazabilidad.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.escorial.trazabilidad.ui.navigation.Routes
import com.escorial.trazabilidad.ui.theme.EkartBlue

@Composable
fun LoginScreen(nav: NavController, vm: LoginViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    var usuario1 by remember { mutableStateOf("") }
    var pass1 by remember { mutableStateOf("") }
    var usuario2 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var verPass by remember { mutableStateOf(false) }

    LaunchedEffect(state.ok) {
        if (state.ok) {
            nav.navigate(Routes.SCAN) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EkartBlue)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Text(
            "Trazabilidad",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Control de Calidad",
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Ingreso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                HorizontalDivider()

                Text("Operario 1", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = usuario1, onValueChange = { usuario1 = it },
                    label = { Text("Usuario") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pass1, onValueChange = { pass1 = it },
                    label = { Text("Contraseña") }, singleLine = true,
                    visualTransformation = if (verPass) androidx.compose.ui.text.input.VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { verPass = !verPass }) {
                            Text(if (verPass) "Ocultar" else "Ver")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(4.dp))
                Text("Operario 2 (opcional)", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = usuario2, onValueChange = { usuario2 = it },
                    label = { Text("Usuario") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pass2, onValueChange = { pass2 = it },
                    label = { Text("Contraseña") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.ingresar(usuario1, pass1, usuario2, pass2) },
                    enabled = !state.cargando,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.cargando) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("INGRESAR", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { nav.navigate(Routes.SETUP) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Configurar servidor") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    state.error?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.limpiarError() },
            confirmButton = { TextButton(onClick = { vm.limpiarError() }) { Text("OK") } },
            title = { Text("Error") },
            text = { Text(msg) },
        )
    }
}
