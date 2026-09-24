package com.escorial.trazabilidad.ui.common

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * Devuelve una funcion para emitir un beep de confirmacion (true) o de error
 * (false). Feedback sonoro para el pickeo con pistola, que no pasa por el
 * escaner ZXing y por lo tanto es silencioso.
 */
@Composable
fun rememberBeep(): (Boolean) -> Unit {
    val tono = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    DisposableEffect(Unit) { onDispose { tono.release() } }
    return { ok ->
        if (ok) tono.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        else tono.startTone(ToneGenerator.TONE_SUP_ERROR, 400)
    }
}
