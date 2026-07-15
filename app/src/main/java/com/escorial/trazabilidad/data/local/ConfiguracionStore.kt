package com.escorial.trazabilidad.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escorial.trazabilidad.domain.ConfiguracionPuesto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "configuracion")

/** Persiste la configuracion del puesto en el dispositivo (reemplaza Configuracion.db). */
class ConfiguracionStore(private val context: Context) {

    private object Keys {
        val INICIALIZADO = booleanPreferencesKey("inicializado")
        val SERVER_URL = stringPreferencesKey("server_url")
        val PLANTA = stringPreferencesKey("planta")
        val TIPO = stringPreferencesKey("tipo")
        val PUESTO_INDEX = intPreferencesKey("puesto_index")
        val PUESTO_NOMBRE = stringPreferencesKey("puesto_nombre")
        val PUESTO_C = intPreferencesKey("puesto_c")
    }

    val configuracion: Flow<ConfiguracionPuesto> = context.dataStore.data.map { p ->
        ConfiguracionPuesto(
            inicializado = p[Keys.INICIALIZADO] ?: false,
            serverUrl = p[Keys.SERVER_URL] ?: "http://10.90.99.114:3000/",
            planta = p[Keys.PLANTA] ?: "25demayo",
            tipo = p[Keys.TIPO] ?: "COCINA",
            puestoIndex = p[Keys.PUESTO_INDEX] ?: 0,
            puestoNombre = p[Keys.PUESTO_NOMBRE] ?: "Reparador",
            puestoC = p[Keys.PUESTO_C] ?: 0,
        )
    }

    suspend fun guardar(config: ConfiguracionPuesto) {
        context.dataStore.edit { p ->
            p[Keys.INICIALIZADO] = config.inicializado
            p[Keys.SERVER_URL] = config.serverUrl
            p[Keys.PLANTA] = config.planta
            p[Keys.TIPO] = config.tipo
            p[Keys.PUESTO_INDEX] = config.puestoIndex
            p[Keys.PUESTO_NOMBRE] = config.puestoNombre
            p[Keys.PUESTO_C] = config.puestoC
        }
    }

    /** Guarda solo los datos de servidor del setup inicial. */
    suspend fun guardarServidor(serverUrl: String, planta: String) {
        context.dataStore.edit { p ->
            p[Keys.INICIALIZADO] = true
            p[Keys.SERVER_URL] = serverUrl
            p[Keys.PLANTA] = planta
        }
    }
}
