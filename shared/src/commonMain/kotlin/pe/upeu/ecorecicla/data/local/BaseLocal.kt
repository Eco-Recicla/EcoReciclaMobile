package pe.upeu.ecorecicla.data.local

import app.cash.sqldelight.db.SqlDriver

class BaseLocal(val driver: SqlDriver) {
    val database = EcoReciclaDatabase(driver)
}
