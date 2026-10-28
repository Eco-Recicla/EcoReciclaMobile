package pe.upeu.ecorecicla.presentation.detalle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.SesionRepository
import pe.upeu.ecorecicla.domain.usecase.AnularPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.CerrarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObtenerPesajesPorPuntoUseCase
import pe.upeu.ecorecicla.domain.usecase.RegistrarOmisionUseCase

data class DetallePuntoUiState(
    val pesajes: List<Pesaje> = emptyList(),
    val cargando: Boolean = false,
    val accionEnProgreso: Boolean = false,
    val mensajeExito: String? = null,
    val mensajeError: String? = null
)

class DetallePuntoViewModel(
    val puntoId: Long,
    private val obtenerPesajesPorPuntoUseCase: ObtenerPesajesPorPuntoUseCase,
    private val anularPesajeUseCase: AnularPesajeUseCase,
    private val registrarOmisionUseCase: RegistrarOmisionUseCase,
    private val cerrarRutaUseCase: CerrarRutaUseCase,
    private val sesionRepository: SesionRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(DetallePuntoUiState(cargando = true))
    val uiState: StateFlow<DetallePuntoUiState> = _uiState.asStateFlow()

    val sesionActual = sesionRepository.sesionActual

    init {
        observarPesajes()
    }

    private fun observarPesajes() {
        scope.launch {
            obtenerPesajesPorPuntoUseCase(puntoId).collect { lista ->
                _uiState.value = _uiState.value.copy(
                    pesajes = lista,
                    cargando = false
                )
            }
        }
    }

    fun tienePrivilegio(privilegio: String): Boolean {
        val sesion = sesionActual.value ?: return false
        return sesion.privilegios.contains(privilegio)
    }

    fun anularPesaje(pesajeId: Long, motivo: String) {
        if (_uiState.value.accionEnProgreso) return
        _uiState.value = _uiState.value.copy(accionEnProgreso = true, mensajeError = null)

        scope.launch {
            val resultado = anularPesajeUseCase(pesajeId, motivo)
            resultado.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        accionEnProgreso = false,
                        mensajeExito = "Pesaje anulado correctamente"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        accionEnProgreso = false,
                        mensajeError = error.message ?: "No se pudo anular el pesaje"
                    )
                }
            )
        }
    }

    fun registrarOmision(motivo: String) {
        if (_uiState.value.accionEnProgreso) return
        _uiState.value = _uiState.value.copy(accionEnProgreso = true, mensajeError = null)

        scope.launch {
            val resultado = registrarOmisionUseCase(puntoId, motivo)
            resultado.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        accionEnProgreso = false,
                        mensajeExito = "Motivo de omisión registrado"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        accionEnProgreso = false,
                        mensajeError = error.message ?: "No se pudo registrar la omisión"
                    )
                }
            )
        }
    }

    fun cerrarRuta() {
        if (_uiState.value.accionEnProgreso) return
        _uiState.value = _uiState.value.copy(accionEnProgreso = true, mensajeError = null)

        scope.launch {
            val resultado = cerrarRutaUseCase()
            resultado.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        accionEnProgreso = false,
                        mensajeExito = "Ruta cerrada exitosamente"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        accionEnProgreso = false,
                        mensajeError = error.message ?: "No se pudo cerrar la ruta"
                    )
                }
            )
        }
    }

    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(mensajeExito = null, mensajeError = null)
    }
}
