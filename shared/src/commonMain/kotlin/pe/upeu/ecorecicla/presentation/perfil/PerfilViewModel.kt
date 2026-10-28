package pe.upeu.ecorecicla.presentation.perfil

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.SesionRepository
import pe.upeu.ecorecicla.domain.usecase.CerrarSesionUseCase

class PerfilViewModel(
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    private val sesionRepository: SesionRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    val sesionActual: StateFlow<Sesion?> = sesionRepository.sesionActual

    fun cerrarSesion(onCompletado: () -> Unit) {
        scope.launch {
            cerrarSesionUseCase()
            onCompletado()
        }
    }
}
