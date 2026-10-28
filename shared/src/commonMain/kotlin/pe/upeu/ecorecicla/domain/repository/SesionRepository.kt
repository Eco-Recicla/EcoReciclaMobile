package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pe.upeu.ecorecicla.domain.model.Sesion

interface SesionRepository {
    val sesionActual: StateFlow<Sesion?>

    suspend fun login(usuario: String, contrasena: String): Result<Sesion>
    suspend fun recuperarSesion(): Sesion?
    suspend fun cerrarSesion()
}
