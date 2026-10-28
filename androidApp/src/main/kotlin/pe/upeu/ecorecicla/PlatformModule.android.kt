package pe.upeu.ecorecicla

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.local.BaseLocal

val platformModule = module {
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<BaseLocal> { BaseLocalAndroid(androidContext()).crearBaseLocal() }
}
