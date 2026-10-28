package pe.upeu.ecorecicla.domain.model

sealed class ErrorApi : Exception() {
    data class Validacion(
        val erroresPorCampo: Map<String, String>,
        override val message: String = "Datos inválidos en el formulario"
    ) : ErrorApi()

    data class NoAutorizado(
        override val message: String = "Sesión no autorizada o token vencido"
    ) : ErrorApi()

    data class SinPrivilegio(
        override val message: String = "No cuenta con el privilegio necesario para esta operación"
    ) : ErrorApi()

    data class NoEncontrado(
        override val message: String = "El recurso solicitado no fue encontrado"
    ) : ErrorApi()

    data class Conflicto(
        override val message: String = "Conflicto con regla de negocio del servidor"
    ) : ErrorApi()

    data class Servidor(
        override val message: String = "Error interno del servidor remoto"
    ) : ErrorApi()

    data class SinConexion(
        override val message: String = "Sin conexión a la red. Se operará en modo offline"
    ) : ErrorApi()

    data class TiempoAgotado(
        override val message: String = "Tiempo de espera agotado al conectar con el servidor"
    ) : ErrorApi()
}
