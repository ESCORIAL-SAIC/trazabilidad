package com.escorial.trazabilidad.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Campo de texto para pistola lectora (teclado en pantalla normal).
 *  - Opcionalmente toma el foco al abrir ([autoFocus]) para pickear de una.
 *  - Al recibir Enter (pistola o teclado): saca el foco y ejecuta [onEnter] con el
 *    texto completo. Usa estado interno para que el Enter no use un valor viejo.
 */
@Composable
fun CampoPickeo(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Number,
    shape: Shape = RoundedCornerShape(8.dp),
    leadingIcon: (@Composable () -> Unit)? = null,
    onEnter: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var texto by remember { mutableStateOf(value) }

    LaunchedEffect(value) { if (value != texto) texto = value }
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    fun alEnter() {
        focusManager.clearFocus()
        onEnter(texto)
    }

    OutlinedTextField(
        value = texto,
        onValueChange = { texto = it; onValueChange(it) },
        label = { Text(label) },
        singleLine = true,
        shape = shape,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { alEnter() }),
        modifier = modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.NumPadEnter)) {
                    alEnter(); true
                } else false
            },
    )
}
