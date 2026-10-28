package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.ResumenSincronizacion
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.EcoReciclaRepository
import pe.upeu.ecorecicla.domain.repository.SesionRepository

class LoginUseCase(private val sesionRepository: SesionRepository) {
    suspend operator fun invoke(usuario: String, contrasena: String): Result<Sesion> {
        return sesionRepository.login(usuario, contrasena)
    }
}

class RecuperarSesionUseCase(private val sesionRepository: SesionRepository) {
    suspend operator fun invoke(): Sesion? {
        return sesionRepository.recuperarSesion()
    }
}

class CerrarSesionUseCase(
    private val sesionRepository: SesionRepository,
    private val ecoReciclaRepository: EcoReciclaRepository
) {
    suspend operator fun invoke() {
        ecoReciclaRepository.limpiarDatosLocales()
        sesionRepository.cerrarSesion()
    }
}

class ObtenerPuntosRutaUseCase(private val repository: EcoReciclaRepository) {
    operator fun invoke(): Flow<List<PuntoAcopio>> {
        return repository.observarPuntosRuta()
    }
}

class ObtenerPesajesPorPuntoUseCase(private val repository: EcoReciclaRepository) {
    operator fun invoke(puntoId: Long): Flow<List<Pesaje>> {
        return repository.observarPesajesPorPunto(puntoId)
    }
}

class RegistrarPesajeUseCase(private val repository: EcoReciclaRepository) {
    suspend operator fun invoke(
        puntoId: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String? = null
    ): Result<Pesaje> {
        return repository.registrarPesaje(puntoId, material, pesoBruto, tara, observacion)
    }
}

class CorregirPesajeUseCase(private val repository: EcoReciclaRepository) {
    suspend operator fun invoke(
        id: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String? = null
    ): Result<Pesaje> {
        return repository.corregirPesaje(id, material, pesoBruto, tara, observacion)
    }
}

class AnularPesajeUseCase(private val repository: EcoReciclaRepository) {
    suspend operator fun invoke(id: Long, motivo: String): Result<Unit> {
        return repository.anularPesaje(id, motivo)
    }
}

class RegistrarOmisionUseCase(private val repository: EcoReciclaRepository) {
    suspend operator fun invoke(puntoId: Long, motivo: String): Result<Unit> {
        return repository.registrarOmision(puntoId, motivo)
    }
}

class CerrarRutaUseCase(private val repository: EcoReciclaRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.cerrarRuta()
    }
}

class SincronizarUseCase(private val repository: EcoReciclaRepository) {
    suspend operator fun invoke(): Result<ResumenSincronizacion> {
        return repository.sincronizar()
    }
}
