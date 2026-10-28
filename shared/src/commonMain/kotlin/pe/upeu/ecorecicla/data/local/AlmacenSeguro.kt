package pe.upeu.ecorecicla.data.local

import pe.upeu.ecorecicla.domain.model.Sesion

interface AlmacenSeguro {
    suspend fun guardarSesion(sesion: Sesion)
    suspend fun obtenerSesion(): Sesion?
    suspend fun limpiarSesion()
}
