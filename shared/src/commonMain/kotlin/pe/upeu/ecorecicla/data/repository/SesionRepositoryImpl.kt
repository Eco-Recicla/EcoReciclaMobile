package pe.upeu.ecorecicla.data.repository

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.remote.ApiConfig
import pe.upeu.ecorecicla.data.remote.ClienteHttp
import pe.upeu.ecorecicla.data.remote.MapeoErrores
import pe.upeu.ecorecicla.data.remote.dto.LoginRequestDto
import pe.upeu.ecorecicla.data.remote.dto.LoginResponseDto
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.SesionRepository

class SesionRepositoryImpl(
    private val clienteHttp: ClienteHttp,
    private val almacenSeguro: AlmacenSeguro
) : SesionRepository {

    private val _sesionActual = MutableStateFlow<Sesion?>(null)
    override val sesionActual: StateFlow<Sesion?> = _sesionActual.asStateFlow()

    override suspend fun login(usuario: String, contrasena: String): Result<Sesion> {
        return try {
            val response = clienteHttp.client.post("${ApiConfig.baseUrl}/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(usuario.trim(), contrasena))
            }

            if (response.status.isSuccess()) {
                val dto = response.body<LoginResponseDto>()
                val sesion = dto.toDomain()
                almacenSeguro.guardarSesion(sesion)
                _sesionActual.value = sesion
                Result.success(sesion)
            } else {
                val error = MapeoErrores.mapearRespuestaHttp(response)
                Result.failure(error)
            }
        } catch (e: Throwable) {
            val error = MapeoErrores.mapearExcepcionRed(e)
            Result.failure(error)
        }
    }

    override suspend fun recuperarSesion(): Sesion? {
        val guardada = almacenSeguro.obtenerSesion()
        _sesionActual.value = guardada
        return guardada
    }

    override suspend fun cerrarSesion() {
        almacenSeguro.limpiarSesion()
        _sesionActual.value = null
    }
}
