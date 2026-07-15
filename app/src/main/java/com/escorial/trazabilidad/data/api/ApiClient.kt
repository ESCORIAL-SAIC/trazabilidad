package com.escorial.trazabilidad.data.api

import com.escorial.trazabilidad.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Volatile
    private var cached: Pair<String, TrazabilidadApi>? = null

    /** Devuelve la Api para la baseUrl vigente (ApiConfig.baseUrl), recreando si cambio. */
    fun api(): TrazabilidadApi {
        val url = ApiConfig.baseUrl
        cached?.let { if (it.first == url) return it.second }
        val nueva = build(url)
        cached = url to nueva
        return nueva
    }

    private fun build(baseUrl: String): TrazabilidadApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        // Inyecta X-Planta en cada request (lee ApiConfig.planta dinamicamente).
        val plantaHeader = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .header("X-Planta", ApiConfig.planta)
                .build()
            chain.proceed(req)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(plantaHeader)
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TrazabilidadApi::class.java)
    }
}
