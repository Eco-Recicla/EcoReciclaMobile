package pe.upeu.ecorecicla.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.remote.ApiConfig
import pe.upeu.ecorecicla.data.remote.ClienteHttp
import pe.upeu.ecorecicla.data.remote.MapeoErrores
import pe.upeu.ecorecicla.data.remote.dto.*
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.EcoReciclaRepository
import pe.upeu.ecorecicla.domain.repository.SesionRepository
import kotlin.math.roundToInt

class EcoReciclaRepositoryImpl(
    private val baseLocal: BaseLocal,
    private val clienteHttp: ClienteHttp,
    private val sesionRepository: SesionRepository
) : EcoReciclaRepository {

    private val db = baseLocal.database
    private val syncMutex = Mutex()

    private fun usuarioActual(): String {
        return sesionRepository.sesionActual.value?.usuario ?: ""
    }

    override fun observarPuntosRuta(): Flow<List<PuntoAcopio>> {
        val user = usuarioActual()
        if (user.isBlank()) return flowOf(emptyList())

        return db.ecoReciclaDatabaseQueries.obtenerPuntosDeRuta(user)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    PuntoAcopio(
                        id = it.id,
                        razonSocial = it.razon_social,
                        direccion = it.direccion,
                        zona = it.zona,
                        materialHabitual = it.material_habitual,
                        secuencia = it.secuencia.toInt(),
                        atendido = it.atendido == 1L
                    )
                }
            }
    }

    override fun observarPesajesPorPunto(puntoId: Long): Flow<List<Pesaje>> {
        val user = usuarioActual()
        return db.ecoReciclaDatabaseQueries.obtenerPesajesPorPunto(puntoId, user)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    val estado = if (it.estado_tipo == "ANULADO") {
                        EstadoPesaje.Anulado(it.motivo_anulacion ?: "Anulado")
                    } else {
                        EstadoPesaje.Registrado
                    }
                    Pesaje(
                        id = it.id,
                        puntoId = it.punto_id,
                        fechaHora = it.fecha_hora,
                        material = it.material,
                        pesoBruto = it.peso_bruto,
                        tara = it.tara,
                        pesoNeto = it.peso_neto,
                        operario = it.operario,
                        estado = estado,
                        pendiente = it.pendiente == 1L
                    )
                }
            }
    }

    override fun observarConteoPendientes(): Flow<Int> {
        val user = usuarioActual()
        if (user.isBlank()) return flowOf(0)

        return db.ecoReciclaDatabaseQueries.contarPesajesPendientes(user)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it.toInt() }
    }

    override fun observarUltimaSincronizacion(): Flow<String?> {
        return db.ecoReciclaDatabaseQueries.obtenerMetadato("ultima_sincronizacion")
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.firstOrNull() }
    }

    override suspend fun registrarPesaje(
        puntoId: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String?
    ): Result<Pesaje> {
        val user = usuarioActual()
        val now = Clock.System.now()
        val isoDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).toString().substringBeforeLast('.')
        val netoEstimado = ((pesoBruto - tara) * 100).roundToInt() / 100.0
        val clientOpId = "local-op-${now.toEpochMilliseconds()}-$puntoId"

        // 1. Guardar primero en la base local con ID provisional negativo y marca pendiente = 1
        val idProvisional: Long = db.transactionWithResult {
            val nextId = db.ecoReciclaDatabaseQueries.obtenerSiguienteIdProvisional().executeAsOne()
            db.ecoReciclaDatabaseQueries.insertarPesaje(
                id = nextId,
                puntoId = puntoId,
                fechaHora = isoDate,
                material = material,
                pesoBruto = pesoBruto,
                tara = tara,
                pesoNeto = netoEstimado,
                operario = user,
                estadoTipo = "REGISTRADO",
                motivoAnulacion = null,
                pendiente = 1L,
                clientOperationId = clientOpId,
                rutaId = null,
                usuarioSesion = user
            )
            db.ecoReciclaDatabaseQueries.marcarPuntoAtendido(puntoId, user)
            nextId
        }

        val pesajeLocal = Pesaje(
            id = idProvisional,
            puntoId = puntoId,
            fechaHora = isoDate,
            material = material,
            pesoBruto = pesoBruto,
            tara = tara,
            pesoNeto = netoEstimado,
            operario = user,
            estado = EstadoPesaje.Registrado,
            pendiente = true
        )

        // 2. Intentar enviar al servidor de inmediato si hay conexión
        return try {
            val response = clienteHttp.client.post("${ApiConfig.baseUrl}/pesajes") {
                contentType(ContentType.Application.Json)
                setBody(
                    PesajeRequestDto(
                        puntoId = puntoId,
                        material = material,
                        pesoBruto = pesoBruto,
                        tara = tara,
                        observacion = observacion,
                        clientOperationId = clientOpId
                    )
                )
            }

            if (response.status.isSuccess()) {
                val dto = response.body<PesajeDto>()
                db.ecoReciclaDatabaseQueries.actualizarIdDefinitivo(
                    nuevoId = dto.id,
                    pesoNetoDefinitivo = dto.pesoNeto,
                    idProvisional = idProvisional
                )
                Result.success(dto.toDomain())
            } else {
                val err = MapeoErrores.mapearRespuestaHttp(response)
                Result.failure(err)
            }
        } catch (_: Throwable) {
            // Sin conexión o timeout: queda guardado localmente como pendiente con su ID provisional negativo
            Result.success(pesajeLocal)
        }
    }

    override suspend fun corregirPesaje(
        id: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String?
    ): Result<Pesaje> {
        val netoEstimado = ((pesoBruto - tara) * 100).roundToInt() / 100.0

        if (id < 0) {
            db.ecoReciclaDatabaseQueries.actualizarPesajeLocal(
                material = material,
                pesoBruto = pesoBruto,
                tara = tara,
                pesoNeto = netoEstimado,
                id = id
            )
            val actualizado = Pesaje(
                id = id,
                puntoId = 0L,
                fechaHora = "",
                material = material,
                pesoBruto = pesoBruto,
                tara = tara,
                pesoNeto = netoEstimado,
                operario = usuarioActual(),
                estado = EstadoPesaje.Registrado,
                pendiente = true
            )
            return Result.success(actualizado)
        }

        return try {
            val response = clienteHttp.client.put("${ApiConfig.baseUrl}/pesajes/$id") {
                contentType(ContentType.Application.Json)
                setBody(
                    PesajeUpdateRequestDto(
                        material = material,
                        pesoBruto = pesoBruto,
                        tara = tara,
                        observacion = observacion
                    )
                )
            }

            if (response.status.isSuccess()) {
                val dto = response.body<PesajeDto>()
                db.ecoReciclaDatabaseQueries.actualizarPesajeLocal(
                    material = dto.material,
                    pesoBruto = dto.pesoBruto,
                    tara = dto.tara,
                    pesoNeto = dto.pesoNeto,
                    id = id
                )
                db.ecoReciclaDatabaseQueries.actualizarIdDefinitivo(
                    nuevoId = id,
                    pesoNetoDefinitivo = dto.pesoNeto,
                    idProvisional = id
                )
                Result.success(dto.toDomain())
            } else {
                Result.failure(MapeoErrores.mapearRespuestaHttp(response))
            }
        } catch (e: Throwable) {
            db.ecoReciclaDatabaseQueries.actualizarPesajeLocal(
                material = material,
                pesoBruto = pesoBruto,
                tara = tara,
                pesoNeto = netoEstimado,
                id = id
            )
            Result.failure(MapeoErrores.mapearExcepcionRed(e))
        }
    }

    override suspend fun anularPesaje(id: Long, motivo: String): Result<Unit> {
        return try {
            val response = clienteHttp.client.delete("${ApiConfig.baseUrl}/pesajes/$id?motivo=${motivo.trim()}")

            if (response.status == io.ktor.http.HttpStatusCode.NoContent) {
                db.ecoReciclaDatabaseQueries.marcarPesajeAnuladoLocal(motivo = motivo.trim(), id = id)
                Result.success(Unit)
            } else {
                Result.failure(MapeoErrores.mapearRespuestaHttp(response))
            }
        } catch (e: Throwable) {
            Result.failure(MapeoErrores.mapearExcepcionRed(e))
        }
    }

    override suspend fun registrarOmision(puntoId: Long, motivo: String): Result<Unit> {
        val user = usuarioActual()
        return try {
            val rutaResp = clienteHttp.client.get("${ApiConfig.baseUrl}/rutas/hoy")
            if (!rutaResp.status.isSuccess()) {
                return Result.failure(MapeoErrores.mapearRespuestaHttp(rutaResp))
            }
            val ruta = rutaResp.body<RutaDto>()

            val response = clienteHttp.client.post("${ApiConfig.baseUrl}/rutas/${ruta.id}/puntos/$puntoId/omision") {
                contentType(ContentType.Application.Json)
                setBody(RegistrarOmisionRequestDto(motivo.trim()))
            }

            if (response.status.isSuccess()) {
                db.ecoReciclaDatabaseQueries.marcarPuntoAtendido(puntoId, user)
                Result.success(Unit)
            } else {
                Result.failure(MapeoErrores.mapearRespuestaHttp(response))
            }
        } catch (e: Throwable) {
            Result.failure(MapeoErrores.mapearExcepcionRed(e))
        }
    }

    override suspend fun cerrarRuta(): Result<Unit> {
        return try {
            val rutaResp = clienteHttp.client.get("${ApiConfig.baseUrl}/rutas/hoy")
            if (!rutaResp.status.isSuccess()) {
                return Result.failure(MapeoErrores.mapearRespuestaHttp(rutaResp))
            }
            val ruta = rutaResp.body<RutaDto>()

            val response = clienteHttp.client.post("${ApiConfig.baseUrl}/rutas/${ruta.id}/cerrar")

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(MapeoErrores.mapearRespuestaHttp(response))
            }
        } catch (e: Throwable) {
            Result.failure(MapeoErrores.mapearExcepcionRed(e))
        }
    }

    override suspend fun sincronizar(): Result<ResumenSincronizacion> = syncMutex.withLock {
        val user = usuarioActual()
        if (user.isBlank()) {
            return Result.failure(ErrorApi.NoAutorizado("Debe iniciar sesión para sincronizar"))
        }

        var enviados = 0
        var confirmados = 0
        var rechazados = 0
        var descargados = 0

        try {
            // PASO 1: Subir registros pendientes antes de descargar
            val pendientes = db.ecoReciclaDatabaseQueries.obtenerPesajesPendientes(user).executeAsList()
            for (p in pendientes) {
                enviados++
                val response = clienteHttp.client.post("${ApiConfig.baseUrl}/pesajes") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        PesajeRequestDto(
                            puntoId = p.punto_id,
                            material = p.material,
                            pesoBruto = p.peso_bruto,
                            tara = p.tara,
                            clientOperationId = p.client_operation_id
                        )
                    )
                }

                if (response.status.isSuccess()) {
                    val dto = response.body<PesajeDto>()
                    db.transaction {
                        db.ecoReciclaDatabaseQueries.actualizarIdDefinitivo(
                            nuevoId = dto.id,
                            pesoNetoDefinitivo = dto.pesoNeto,
                            idProvisional = p.id
                        )
                    }
                    confirmados++
                } else {
                    rechazados++
                }
            }

            // PASO 2: Descargar ruta del día actualizada y actualizar SQLite
            val rutaResp = clienteHttp.client.get("${ApiConfig.baseUrl}/rutas/hoy")
            if (rutaResp.status.isSuccess()) {
                val rutaDto = rutaResp.body<RutaDto>()
                db.transaction {
                    for (punto in rutaDto.puntos) {
                        db.ecoReciclaDatabaseQueries.insertarOReemplazarPunto(
                            id = punto.id,
                            razonSocial = punto.razonSocial,
                            direccion = punto.direccion,
                            zona = punto.zona,
                            materialHabitual = punto.materialHabitual,
                            secuencia = punto.secuencia.toLong(),
                            atendido = if (punto.atendido) 1L else 0L,
                            rutaId = rutaDto.id,
                            usuario = user
                        )
                    }
                }

                // PASO 3: Descargar pesajes de cada punto y actualizar sin sobreescribir pendientes
                for (punto in rutaDto.puntos) {
                    val pesajesResp = clienteHttp.client.get("${ApiConfig.baseUrl}/puntos/${punto.id}/pesajes?tamanio=50")
                    if (pesajesResp.status.isSuccess()) {
                        val paginaPesajes = pesajesResp.body<PaginaRespuestaDto<PesajeDto>>()
                        db.transaction {
                            for (pDto in paginaPesajes.contenido) {
                                db.ecoReciclaDatabaseQueries.insertarOReemplazarPesaje(
                                    id = pDto.id,
                                    puntoId = pDto.puntoId,
                                    fechaHora = pDto.fechaHora,
                                    material = pDto.material,
                                    pesoBruto = pDto.pesoBruto,
                                    tara = pDto.tara,
                                    pesoNeto = pDto.pesoNeto,
                                    operario = pDto.operario,
                                    estadoTipo = pDto.estado.tipo,
                                    motivoAnulacion = pDto.estado.motivo,
                                    pendiente = 0L,
                                    clientOperationId = null,
                                    rutaId = rutaDto.id,
                                    usuarioSesion = user
                                )
                                descargados++
                            }
                        }
                    }
                }
            }

            // PASO 4: Persistir marca de última sincronización correcta
            val timestampNow = Clock.System.now().toString()
            db.ecoReciclaDatabaseQueries.guardarMetadato("ultima_sincronizacion", timestampNow)

            val resumen = ResumenSincronizacion(
                enviados = enviados,
                confirmados = confirmados,
                rechazados = rechazados,
                descargados = descargados,
                fechaHora = timestampNow
            )
            Result.success(resumen)
        } catch (e: Throwable) {
            Result.failure(MapeoErrores.mapearExcepcionRed(e))
        }
    }

    override suspend fun limpiarDatosLocales() {
        val user = usuarioActual()
        if (user.isNotBlank()) {
            db.transaction {
                db.ecoReciclaDatabaseQueries.limpiarPuntosPorUsuario(user)
                db.ecoReciclaDatabaseQueries.eliminarPesajesPorUsuario(user)
                db.ecoReciclaDatabaseQueries.limpiarMetadatos()
            }
        }
    }
}
