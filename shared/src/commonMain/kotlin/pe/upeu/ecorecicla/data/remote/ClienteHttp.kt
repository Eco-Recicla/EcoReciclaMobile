package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.data.local.AlmacenSeguro

class ClienteHttp(
    val engine: HttpClientEngine? = null,
    val almacenSeguro: AlmacenSeguro
) {
    val client: HttpClient by lazy {
        val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = false
                        encodeDefaults = true
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val sesion = almacenSeguro.obtenerSesion()
                        if (sesion != null && sesion.token.isNotBlank()) {
                            BearerTokens(accessToken = sesion.token, refreshToken = "")
                        } else {
                            null
                        }
                    }
                    sendWithoutRequest { request ->
                        // Envío anticipado del token únicamente a nuestro backend autorizado
                        request.url.host.contains("10.0.2.2") ||
                                request.url.host.contains("localhost") ||
                                request.url.host.contains("127.0.0.1") ||
                                request.url.host.contains("ecorecicla")
                    }
                }
            }

            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        // Saneamiento de credenciales y autorización para no registrar tokens ni contraseñas
                        val saneado = message
                            .replace(Regex("Bearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+"), "Bearer [PROTEGIDO]")
                            .replace(Regex("\"contrasena\":\\s*\"[^\"]+\""), "\"contrasena\": \"[PROTEGIDO]\"")
                            .replace(Regex("\"password\":\\s*\"[^\"]+\""), "\"password\": \"[PROTEGIDO]\"")
                        println("[KtorClient] $saneado")
                    }
                }
            }
        }

        if (engine != null) {
            HttpClient(engine, config)
        } else {
            HttpClient(config)
        }
    }
}
