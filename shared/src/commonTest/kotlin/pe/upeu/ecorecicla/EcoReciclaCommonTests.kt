package pe.upeu.ecorecicla

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.local.EcoReciclaDatabase
import pe.upeu.ecorecicla.data.remote.ClienteHttp
import pe.upeu.ecorecicla.data.remote.MapeoErrores
import pe.upeu.ecorecicla.data.remote.dto.EstadoPesajeDto
import pe.upeu.ecorecicla.data.remote.dto.LoginResponseDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PuntoAcopioDto
import pe.upeu.ecorecicla.data.repository.EcoReciclaRepositoryImpl
import pe.upeu.ecorecicla.data.repository.SesionRepositoryImpl
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Sesion
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeAlmacenSeguro : AlmacenSeguro {
    private var sesionGuardada: Sesion? = null

    override suspend fun guardarSesion(sesion: Sesion) {
        sesionGuardada = sesion
    }

    override suspend fun obtenerSesion(): Sesion? {
        return sesionGuardada
    }

    override suspend fun limpiarSesion() {
        sesionGuardada = null
    }
}

class EcoReciclaCommonTests {

    private lateinit var baseLocal: BaseLocal
    private lateinit var fakeAlmacen: FakeAlmacenSeguro

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EcoReciclaDatabase.Schema.create(driver)
        baseLocal = BaseLocal(driver)
        fakeAlmacen = FakeAlmacenSeguro()
    }

    @Test
    fun `test1 Conversion de DTO a Dominio`() {
        // 1. DTO de Punto de Acopio
        val puntoDto = PuntoAcopioDto(
            id = 10L,
            razonSocial = "Plásticos Los Andes",
            direccion = "Av. Nicolás Ayllón 123",
            zona = "Este",
            materialHabitual = "Plástico",
            secuencia = 3,
            atendido = true
        )
        val puntoDominio = puntoDto.toDomain()
        assertEquals(10L, puntoDominio.id)
        assertEquals("Plásticos Los Andes", puntoDominio.razonSocial)
        assertEquals(3, puntoDominio.secuencia)
        assertTrue(puntoDominio.atendido)

        // 2. DTO de Pesaje Registrado
        val pesajeDto = PesajeDto(
            id = 50L,
            puntoId = 10L,
            fechaHora = "2026-10-28T10:00:00",
            material = "Plástico PET",
            pesoBruto = 150.0,
            tara = 15.0,
            pesoNeto = 135.0,
            operario = "operario1",
            estado = EstadoPesajeDto(tipo = "REGISTRADO"),
            pendiente = false
        )
        val pesajeDominio = pesajeDto.toDomain()
        assertEquals(50L, pesajeDominio.id)
        assertEquals(135.0, pesajeDominio.pesoNeto)
        assertEquals(EstadoPesaje.Registrado, pesajeDominio.estado)

        // 3. DTO de Pesaje Anulado
        val pesajeAnuladoDto = pesajeDto.copy(
            id = 51L,
            estado = EstadoPesajeDto(tipo = "ANULADO", motivo = "Balanza dañada")
        )
        val pesajeAnuladoDominio = pesajeAnuladoDto.toDomain()
        assertIs<EstadoPesaje.Anulado>(pesajeAnuladoDominio.estado)
        assertEquals("Balanza dañada", (pesajeAnuladoDominio.estado as EstadoPesaje.Anulado).motivo)

        // 4. DTO de Sesión
        val loginDto = LoginResponseDto(
            token = "jwt.token.falso",
            usuario = "operario1",
            rol = "OPERARIO",
            privilegios = listOf("PUNTO_LEER", "PESAJE_CREAR")
        )
        val sesionDominio = loginDto.toDomain()
        assertEquals("jwt.token.falso", sesionDominio.token)
        assertTrue(sesionDominio.privilegios.contains("PESAJE_CREAR"))
    }

    @Test
    fun `test2 Traduccion de Errores HTTP a ErrorApi`() = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v1/bad-request" -> respond(
                    content = """{"timestamp":"2026-10-28T10:00:00","status":400,"error":"Bad Request","message":"Datos invalidos","path":"/api/v1/pesajes","validationErrors":{"tara":"La tara debe ser menor que el bruto"}}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/api/v1/unauthorized" -> respond(
                    content = """{"timestamp":"2026-10-28T10:00:00","status":401,"error":"Unauthorized","message":"Token ausente o vencido","path":"/api/v1/auth/me"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/api/v1/conflict" -> respond(
                    content = """{"timestamp":"2026-10-28T10:00:00","status":409,"error":"Conflict","message":"No se puede registrar pesajes en una ruta ya cerrada","path":"/api/v1/pesajes"}""",
                    status = HttpStatusCode.Conflict,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(content = "Error", status = HttpStatusCode.InternalServerError)
            }
        }

        val clienteHttp = ClienteHttp(engine = mockEngine, almacenSeguro = fakeAlmacen)

        // Comprobar 400
        val resp400 = clienteHttp.client.get("http://localhost/api/v1/bad-request")
        val error400 = MapeoErrores.mapearRespuestaHttp(resp400)
        assertIs<ErrorApi.Validacion>(error400)
        assertEquals("La tara debe ser menor que el bruto", error400.erroresPorCampo["tara"])

        // Comprobar 401
        val resp401 = clienteHttp.client.get("http://localhost/api/v1/unauthorized")
        val error401 = MapeoErrores.mapearRespuestaHttp(resp401)
        assertIs<ErrorApi.NoAutorizado>(error401)
        assertEquals("Token ausente o vencido", error401.message)

        // Comprobar 409
        val resp409 = clienteHttp.client.get("http://localhost/api/v1/conflict")
        val error409 = MapeoErrores.mapearRespuestaHttp(resp409)
        assertIs<ErrorApi.Conflicto>(error409)
        assertEquals("No se puede registrar pesajes en una ruta ya cerrada", error409.message)
    }

    @Test
    fun `test3 Registro sin conexion queda guardado con ID negativo y marca pendiente`() = runTest {
        val mockEngine = MockEngine {
            throw RuntimeException("Connection refused (simulando offline)")
        }

        fakeAlmacen.guardarSesion(
            Sesion("token.prueba", "operario1", "OPERARIO", setOf("PESAJE_CREAR"))
        )

        val clienteHttp = ClienteHttp(engine = mockEngine, almacenSeguro = fakeAlmacen)
        val sesionRepo = SesionRepositoryImpl(clienteHttp, fakeAlmacen)
        sesionRepo.recuperarSesion()

        val repository = EcoReciclaRepositoryImpl(baseLocal, clienteHttp, sesionRepo)

        val resultado = repository.registrarPesaje(
            puntoId = 1L,
            material = "Plástico PET",
            pesoBruto = 120.0,
            tara = 12.0
        )

        assertTrue(resultado.isSuccess)
        val pesajeGuardado = resultado.getOrNull()!!

        // Verificaciones obligatorias:
        assertTrue(pesajeGuardado.id < 0, "El ID provisional debe ser negativo mientras esté offline")
        assertTrue(pesajeGuardado.pendiente, "El pesaje debe estar marcado como pendiente")
        assertEquals(108.0, pesajeGuardado.pesoNeto)

        // Verificar que quedó persistido en la BD SQLite local
        val pesajesEnDb = repository.observarPesajesPorPunto(1L).first()
        assertEquals(1, pesajesEnDb.size)
        assertEquals(pesajeGuardado.id, pesajesEnDb.first().id)
        assertTrue(pesajesEnDb.first().pendiente)
    }

    @Test
    fun `test4 Orden de sincronizacion envio antes de descarga y sustitucion de ID provisional`() = runTest {
        val ordenEjecucion = mutableListOf<String>()

        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/pesajes") && request.method.value == "POST" -> {
                    ordenEjecucion.add("POST_PESAJE_PENDIENTE")
                    respond(
                        content = """{"id":999,"puntoId":1,"fechaHora":"2026-10-28T10:00:00","material":"Plástico","pesoBruto":100.0,"tara":10.0,"pesoNeto":90.0,"operario":"operario1","estado":{"tipo":"REGISTRADO"},"pendiente":false}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path.endsWith("/rutas/hoy") -> {
                    ordenEjecucion.add("GET_RUTA_HOY")
                    respond(
                        content = """{"id":1,"fecha":"2026-10-28","estado":"ABIERTA","usuarioAsignado":"operario1","puntos":[{"id":1,"razonSocial":"Punto 1","direccion":"Dir 1","zona":"Norte","materialHabitual":"Plástico","secuencia":1,"atendido":true}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                path.contains("/puntos/1/pesajes") -> {
                    ordenEjecucion.add("GET_PESAJES_PUNTO")
                    respond(
                        content = """{"contenido":[{"id":999,"puntoId":1,"fechaHora":"2026-10-28T10:00:00","material":"Plástico","pesoBruto":100.0,"tara":10.0,"pesoNeto":90.0,"operario":"operario1","estado":{"tipo":"REGISTRADO"},"pendiente":false}],"pagina":0,"tamanio":50,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(content = "{}", status = HttpStatusCode.OK)
            }
        }

        fakeAlmacen.guardarSesion(
            Sesion("token.prueba", "operario1", "OPERARIO", setOf("PESAJE_CREAR"))
        )

        val clienteHttp = ClienteHttp(engine = mockEngine, almacenSeguro = fakeAlmacen)
        val sesionRepo = SesionRepositoryImpl(clienteHttp, fakeAlmacen)
        sesionRepo.recuperarSesion()

        val repository = EcoReciclaRepositoryImpl(baseLocal, clienteHttp, sesionRepo)

        // Insertar un pesaje offline con ID provisional negativo (-1)
        baseLocal.database.ecoReciclaDatabaseQueries.insertarPesaje(
            id = -1L,
            puntoId = 1L,
            fechaHora = "2026-10-28T09:00:00",
            material = "Plástico",
            pesoBruto = 100.0,
            tara = 10.0,
            pesoNeto = 90.0,
            operario = "operario1",
            estadoTipo = "REGISTRADO",
            motivoAnulacion = null,
            pendiente = 1L,
            clientOperationId = "op-test-sync",
            rutaId = null,
            usuarioSesion = "operario1"
        )

        // Ejecutar sincronización
        val resultadoSync = repository.sincronizar()
        assertTrue(resultadoSync.isSuccess)

        // 1. Verificar orden: el envío (POST) debe ocurrir ANTES de la descarga (GET)
        assertEquals("POST_PESAJE_PENDIENTE", ordenEjecucion[0])
        assertEquals("GET_RUTA_HOY", ordenEjecucion[1])
        assertEquals("GET_PESAJES_PUNTO", ordenEjecucion[2])

        // 2. Verificar que el ID provisional (-1) fue sustituido por el ID definitivo (999) en SQLite
        val pesajesEnDb = repository.observarPesajesPorPunto(1L).first()
        assertEquals(1, pesajesEnDb.size)
        assertEquals(999L, pesajesEnDb.first().id)
        assertEquals(false, pesajesEnDb.first().pendiente)
    }
}
