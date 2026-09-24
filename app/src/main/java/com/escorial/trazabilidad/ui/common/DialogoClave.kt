package com.escorial.trazabilidad.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.escorial.trazabilidad.BuildConfig
import java.security.MessageDigest

/**
 * Hash SHA-256 en minusculas de [texto], para comparar contra BuildConfig.CONFIG_PASSWORD_SHA256
 * sin que la clave exista en texto plano en el binario.
 */
fun sha256(texto: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(texto.toByteArray())
        .joinToString("") { "%02x".format(it) }

/**
 * True si [clave] es la clave de acceso a Configuracion.
 *
 * Con hash vacio (compilacion sin config.properties ni -PCONFIG_PASSWORD_SHA256) no hay
 * clave configurada y el acceso queda libre: es lo comodo en desarrollo, y evita dejar
 * afuera al de planta si un build sale sin el secret.
 */
fun claveConfigValida(clave: String): Boolean {
    val esperado = BuildConfig.CONFIG_PASSWORD_SHA256
    if (esperado.isBlank()) return true
    return sha256(clave).equals(esperado, ignoreCase = true)
}

/** True si esta compilacion exige clave para entrar a Configuracion. */
fun configPideClave(): Boolean = BuildConfig.CONFIG_PASSWORD_SHA256.isNotBlank()

/**
 * Pide la clave de Configuracion. Llama a [onOk] solo si es correcta; si no, muestra el
 * error y deja reintentar (no bloquea ni cuenta intentos: es una barrera contra el toque
 * curioso en el piso, no un control de seguridad).
 */
@Composable
fun DialogoClaveConfig(onOk: () -> Unit, onCancelar: () -> Unit) {
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun confirmar() {
        if (claveConfigValida(clave)) onOk() else { error = true; clave = "" }
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Configuración") },
        text = {
            Column {
                Text("Ingrese la clave de acceso.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = clave,
                    onValueChange = { clave = it; error = false },
                    label = { Text("Clave") },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { confirmar() }),
                    supportingText = if (error) {
                        { Text("Clave incorrecta.") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { confirmar() }) { Text("Ingresar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
