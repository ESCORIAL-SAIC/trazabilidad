package com.escorial.trazabilidad.ui.common

import androidx.compose.ui.graphics.Color

/**
 * Convierte un color "#RRGGBB" (como vp_etiquetas_todos.color) a Color de Compose.
 * Replica RGBToColor() del Delphi. Devuelve [fallback] si el formato es invalido.
 */
fun parseColorRRGGBB(rgb: String?, fallback: Color = Color(0xFF1E88E5)): Color {
    if (rgb == null || rgb.length != 7 || !rgb.startsWith("#")) return fallback
    return try {
        val r = rgb.substring(1, 3).toInt(16)
        val g = rgb.substring(3, 5).toInt(16)
        val b = rgb.substring(5, 7).toInt(16)
        Color(r, g, b)
    } catch (e: Exception) {
        fallback
    }
}
