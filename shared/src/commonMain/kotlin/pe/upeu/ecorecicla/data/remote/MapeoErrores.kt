package pe.upeu.ecorecicla.data.remote

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.upeu.ecorecicla.domain.model.ErrorApi

object MapeoErrores {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun mapearRespuestaHttp(response: HttpResponse): ErrorApi {
        val status = response.status
        val bodyText = try {
            response.bodyAsText()
        } catch (e: Exception) {
            ""
        }

        var mensajeServidor = ""
        var validationErrors = mutableMapOf<String, String>()

        if (bodyText.isNotBlank()) {
            try {
                val element = json.parseToJsonElement(bodyText).jsonObject
                mensajeServidor = element["message"]?.jsonPrimitive?.content ?: ""

                val valErrorsElement = element["validationErrors"]?.jsonObject
                valErrorsElement?.forEach { (campo, errorJson) ->
                    validationErrors[campo] = errorJson.jsonPrimitive.content
                }
            } catch (_: Exception) {
                mensajeServidor = bodyText
            }
        }

        return when (status) {
            HttpStatusCode.BadRequest -> {
                val msg = if (mensajeServidor.isNotBlank()) mensajeServidor else "Datos inválidos en el formulario"
                ErrorApi.Validacion(validationErrors, msg)
            }
            HttpStatusCode.Unauthorized -> {
                ErrorApi.NoAutorizado(if (mensajeServidor.isNotBlank()) mensajeServidor else "Credenciales incorrectas o sesión vencida")
            }
            HttpStatusCode.Forbidden -> {
                ErrorApi.SinPrivilegio(if (mensajeServidor.isNotBlank()) mensajeServidor else "No cuenta con el privilegio requerido")
            }
            HttpStatusCode.NotFound -> {
                ErrorApi.NoEncontrado(if (mensajeServidor.isNotBlank()) mensajeServidor else "Recurso no encontrado")
            }
            HttpStatusCode.Conflict -> {
                ErrorApi.Conflicto(if (mensajeServidor.isNotBlank()) mensajeServidor else "Conflicto con regla de negocio")
            }
            HttpStatusCode.InternalServerError -> {
                ErrorApi.Servidor(if (mensajeServidor.isNotBlank()) mensajeServidor else "Error interno en el servidor remoto")
            }
            else -> {
                ErrorApi.Servidor("Error HTTP ${status.value}: $mensajeServidor")
            }
        }
    }

    fun mapearExcepcionRed(throwable: Throwable): ErrorApi {
        return when (throwable) {
            is ErrorApi -> throwable
            is HttpRequestTimeoutException -> ErrorApi.TiempoAgotado("Tiempo de espera agotado al comunicar con el servidor")
            else -> {
                val msg = throwable.message?.lowercase() ?: ""
                if (msg.contains("connect") || msg.contains("host") || msg.contains("network") || msg.contains("unreachable") || msg.contains("connection")) {
                    ErrorApi.SinConexion("Sin conexión a la red. Se operará en modo offline")
                } else {
                    ErrorApi.SinConexion("Fallo de conectividad con el servidor EcoAPI")
                }
            }
        }
    }
}
