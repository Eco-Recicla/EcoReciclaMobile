package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.Sesion

@Serializable
data class LoginRequestDto(
    val usuario: String,
    val contrasena: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val usuario: String,
    val rol: String,
    val privilegios: List<String>
) {
    fun toDomain(): Sesion {
        return Sesion(
            token = token,
            usuario = usuario,
            rol = rol,
            privilegios = privilegios.toSet()
        )
    }
}

@Serializable
data class PuntoAcopioDto(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int = 0,
    val atendido: Boolean = false
) {
    fun toDomain(): PuntoAcopio {
        return PuntoAcopio(
            id = id,
            razonSocial = razonSocial,
            direccion = direccion,
            zona = zona,
            materialHabitual = materialHabitual,
            secuencia = secuencia,
            atendido = atendido
        )
    }
}

@Serializable
data class RutaDto(
    val id: Long,
    val fecha: String,
    val estado: String,
    val usuarioAsignado: String,
    val puntos: List<PuntoAcopioDto> = emptyList()
)

@Serializable
data class EstadoPesajeDto(
    val tipo: String,
    val motivo: String? = null
)

@Serializable
data class PesajeDto(
    val id: Long,
    val puntoId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val operario: String,
    val estado: EstadoPesajeDto,
    val pendiente: Boolean = false
) {
    fun toDomain(): Pesaje {
        val estadoModel = when (estado.tipo) {
            "ANULADO" -> EstadoPesaje.Anulado(estado.motivo ?: "Anulado")
            else -> EstadoPesaje.Registrado
        }

        return Pesaje(
            id = id,
            puntoId = puntoId,
            fechaHora = fechaHora,
            material = material,
            pesoBruto = pesoBruto,
            tara = tara,
            pesoNeto = pesoNeto,
            operario = operario,
            estado = estadoModel,
            pendiente = pendiente
        )
    }
}

@Serializable
data class PesajeRequestDto(
    val puntoId: Long,
    val rutaId: Long? = null,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null,
    val clientOperationId: String? = null
)

@Serializable
data class PesajeUpdateRequestDto(
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null
)

@Serializable
data class RegistrarOmisionRequestDto(
    val motivo: String
)

@Serializable
data class PaginaRespuestaDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)
