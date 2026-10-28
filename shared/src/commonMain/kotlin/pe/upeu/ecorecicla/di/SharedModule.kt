package pe.upeu.ecorecicla.di

import org.koin.dsl.module
import pe.upeu.ecorecicla.data.remote.ClienteHttp
import pe.upeu.ecorecicla.data.repository.EcoReciclaRepositoryImpl
import pe.upeu.ecorecicla.data.repository.SesionRepositoryImpl
import pe.upeu.ecorecicla.domain.repository.EcoReciclaRepository
import pe.upeu.ecorecicla.domain.repository.SesionRepository
import pe.upeu.ecorecicla.domain.usecase.*
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoViewModel
import pe.upeu.ecorecicla.presentation.formulario.FormularioPesajeViewModel
import pe.upeu.ecorecicla.presentation.listado.RutaViewModel
import pe.upeu.ecorecicla.presentation.perfil.PerfilViewModel
import pe.upeu.ecorecicla.presentation.sesion.LoginViewModel

val redModule = module {
    single { ClienteHttp(almacenSeguro = get()) }
}

val dataModule = module {
    single<SesionRepository> { SesionRepositoryImpl(clienteHttp = get(), almacenSeguro = get()) }
    single<EcoReciclaRepository> { EcoReciclaRepositoryImpl(baseLocal = get(), clienteHttp = get(), sesionRepository = get()) }
}

val domainModule = module {
    factory { LoginUseCase(get()) }
    factory { RecuperarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get(), get()) }
    factory { ObtenerPuntosRutaUseCase(get()) }
    factory { ObtenerPesajesPorPuntoUseCase(get()) }
    factory { RegistrarPesajeUseCase(get()) }
    factory { CorregirPesajeUseCase(get()) }
    factory { AnularPesajeUseCase(get()) }
    factory { RegistrarOmisionUseCase(get()) }
    factory { CerrarRutaUseCase(get()) }
    factory { SincronizarUseCase(get()) }
}

val presentationModule = module {
    factory { LoginViewModel(get(), get(), get()) }
    factory { RutaViewModel(get(), get(), get(), get()) }
    factory { (puntoId: Long) -> DetallePuntoViewModel(puntoId, get(), get(), get(), get(), get()) }
    factory { (puntoId: Long, pesajeId: Long?) -> FormularioPesajeViewModel(puntoId, pesajeId, get(), get()) }
    factory { PerfilViewModel(get(), get()) }
}
