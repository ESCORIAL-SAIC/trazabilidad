package com.escorial.trazabilidad.data.repo

import com.escorial.trazabilidad.data.api.ApiClient
import com.escorial.trazabilidad.data.api.dto.*

/**
 * Punto unico de acceso a la API. Toma siempre la baseUrl/planta vigentes
 * (ApiClient.api()). Las pantallas no acceden a bases de datos.
 */
class TrazabilidadRepository {
    private val api get() = ApiClient.api()

    suspend fun login(req: LoginRequest): LoginResponse = api.login(req)
    suspend fun plantas(): List<PlantaDto> = api.plantas()
    suspend fun puestos(tipo: String): List<PuestoDto> = api.puestos(tipo)
    suspend fun resolver(req: ResolverRequest): ResolverResponse = api.resolver(req)
    suspend fun nivel2(nivel1Id: String): List<FallaNivel2Dto> = api.nivel2(nivel1Id)
    suspend fun nivel3(nivel2Id: String): List<FallaNivel3Dto> = api.nivel3(nivel2Id)
    suspend fun controlOk(req: ControlOkRequest): ControlOkResponse = api.controlOk(req)
    suspend fun controlFalla(req: ControlFallaRequest): MensajeResponse = api.controlFalla(req)
    suspend fun controlReparacion(req: ControlReparacionRequest): MensajeResponse =
        api.controlReparacion(req)
}
