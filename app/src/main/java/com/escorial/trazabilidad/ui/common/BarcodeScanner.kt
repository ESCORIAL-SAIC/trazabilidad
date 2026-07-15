package com.escorial.trazabilidad.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Devuelve una funcion para lanzar el escaner ZXing. Soporta los mismos formatos
 * que la app Delphi (Code39/93/128, EAN, ITF, UPC, QR, DataMatrix). La
 * CaptureActivity de zxing-android-embedded gestiona el permiso de camara.
 */
@Composable
fun rememberEscaner(onResult: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { onResult(it) }
    }
    return {
        launcher.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                setOrientationLocked(false)
                setBeepEnabled(true)
                setPrompt("Escanear código")
            },
        )
    }
}
