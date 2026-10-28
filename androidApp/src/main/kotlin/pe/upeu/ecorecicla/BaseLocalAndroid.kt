package pe.upeu.ecorecicla

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.local.EcoReciclaDatabase

class BaseLocalAndroid(private val context: Context) {
    fun crearBaseLocal(): BaseLocal {
        val driver = AndroidSqliteDriver(
            schema = EcoReciclaDatabase.Schema,
            context = context,
            name = "ecorecicla.db"
        )
        return BaseLocal(driver)
    }
}
