package pe.upeu.ecorecicla

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import pe.upeu.ecorecicla.di.dataModule
import pe.upeu.ecorecicla.di.domainModule
import pe.upeu.ecorecicla.di.presentationModule
import pe.upeu.ecorecicla.di.redModule

class EcoReciclaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@EcoReciclaApp)
            modules(
                platformModule,
                redModule,
                dataModule,
                domainModule,
                presentationModule
            )
        }
    }
}
