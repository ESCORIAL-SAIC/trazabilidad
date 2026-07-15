package com.escorial.trazabilidad.data.api

import com.escorial.trazabilidad.data.api.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TrazabilidadApi {

    @GET("plantas")
    suspend fun plantas(): List<PlantaDto>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("puestos")
    suspend fun puestos(@Query("tipo") tipo: String): List<PuestoDto>

    @GET("etiquetas/{numero}")
    suspend fun etiqueta(@Path("numero") numero: Int, @Query("tipo") tipo: String): EtiquetaDto

    @GET("fallas/nivel1")
    suspend fun nivel1(@Query("puesto") puesto: String, @Query("tipo") tipo: String): List<FallaNivel1Dto>

    @GET("fallas/nivel2")
    suspend fun nivel2(@Query("nivel1Id") nivel1Id: String): List<FallaNivel2Dto>

    @GET("fallas/nivel3")
    suspend fun nivel3(@Query("nivel2Id") nivel2Id: String): List<FallaNivel3Dto>

    @POST("scan/resolver")
    suspend fun resolver(@Body body: ResolverRequest): ResolverResponse

    @POST("control/ok")
    suspend fun controlOk(@Body body: ControlOkRequest): ControlOkResponse

    @POST("control/falla")
    suspend fun controlFalla(@Body body: ControlFallaRequest): MensajeResponse

    @POST("control/reparacion")
    suspend fun controlReparacion(@Body body: ControlReparacionRequest): MensajeResponse
}
