package pe.upeu.ecorecicla.presentation.listado

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.ResumenSincronizacion
import pe.upeu.ecorecicla.domain.repository.EcoReciclaRepository
import pe.upeu.ecorecicla.domain.repository.SesionRepository
import pe.upeu.ecorecicla.domain.usecase.ObtenerPuntosRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase

data class RutaUiState(
    val puntos: List<PuntoAcopio> = emptyList(),
    val cargando: Boolean = false,
    val sincronizando: Boolean = false,
    val pendientesCount: Int = 0,
    val ultimaSincronizacionTexto: String = "Nunca sincronizado",
    val mensajeResumen: String? = null,
    val mensajeError: String? = null
)

class RutaViewModel(
    private val obtenerPuntosRutaUseCase: ObtenerPuntosRutaUseCase,
    private val sincronizarUseCase: SincronizarUseCase,
    private val ecoReciclaRepository: EcoReciclaRepository,
    private val sesionRepository: SesionRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(RutaUiState(cargando = true))
    val uiState: StateFlow<RutaUiState> = _uiState.asStateFlow()

    val sesionActual = sesionRepository.sesionActual

    init {
        observarBaseLocal()
    }

    private fun observarBaseLocal() {
        // La pantalla principal observa exclusivamente la base de datos local
        scope.launch {
            obtenerPuntosRutaUseCase().collect { lista ->
                _uiState.value = _uiState.value.copy(
                    puntos = lista,
                    cargando = false
                )
            }
        }

        scope.launch {
            ecoReciclaRepository.observarConteoPendientes().collect { count ->
                _uiState.value = _uiState.value.copy(pendientesCount = count)
            }
        }

        scope.launch {
            ecoReciclaRepository.observarUltimaSincronizacion().collect { ts ->
                val texto = formatearTiempoTranscurrido(ts)
                _uiState.value = _uiState.value.copy(ultimaSincronizacionTexto = texto)
            }
        }
    }

    fun sincronizar() {
        if (_uiState.value.sincronizando) return
        _uiState.value = _uiState.value.copy(sincronizando = true, mensajeResumen = null, mensajeError = null)

        scope.launch {
            val resultado = sincronizarUseCase()
            resultado.fold(
                onSuccess = { resumen ->
                    val detalle = "Sincronizado: ${resumen.confirmados} enviados, ${resumen.descargados} recibidos"
                    _uiState.value = _uiState.value.copy(
                        sincronizando = false,
                        mensajeResumen = detalle
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        sincronizando = false,
                        mensajeError = error.message ?: "Fallo al sincronizar"
                    )
                }
            )
        }
    }

    fun limpiarMensajeResumen() {
        _uiState.value = _uiState.value.copy(mensajeResumen = null, mensajeError = null)
    }

    private fun formatearTiempoTranscurrido(timestamp: String?): String {
        if (timestamp.isNullOrBlank()) return "Nunca sincronizado"
        return try {
            val past = Instant.parse(timestamp)
            val now = Clock.System.now()
            val diffSeconds = (now - past).inWholeSeconds
            when {
                diffSeconds < 60 -> "Hace instantes"
                diffSeconds < 3600 -> "Hace ${diffSeconds / 60} min"
                diffSeconds < 86400 -> "Hace ${diffSeconds / 3600} h"
                else -> "Hace ${diffSeconds / 86400} d"
            }
        } catch (_: Exception) {
            "Recientemente"
        }
    }
}
