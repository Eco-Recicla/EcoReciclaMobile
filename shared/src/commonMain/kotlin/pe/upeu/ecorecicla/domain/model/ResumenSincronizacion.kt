package pe.upeu.ecorecicla.domain.model

data class ResumenSincronizacion(
    val enviados: Int = 0,
    val confirmados: Int = 0,
    val rechazados: Int = 0,
    val descargados: Int = 0,
    val fechaHora: String = ""
)
