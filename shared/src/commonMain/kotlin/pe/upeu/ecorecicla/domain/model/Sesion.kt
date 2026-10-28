package pe.upeu.ecorecicla.domain.model

data class Sesion(
    val token: String,
    val usuario: String,
    val rol: String,
    val privilegios: Set<String>
)
