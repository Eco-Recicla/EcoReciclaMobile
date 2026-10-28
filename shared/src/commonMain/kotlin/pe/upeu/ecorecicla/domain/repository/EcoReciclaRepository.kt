package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.ResumenSincronizacion

interface EcoReciclaRepository {
    fun observarPuntosRuta(): Flow<List<PuntoAcopio>>
    fun observarPesajesPorPunto(puntoId: Long): Flow<List<Pesaje>>
    fun observarConteoPendientes(): Flow<Int>
    fun observarUltimaSincronizacion(): Flow<String?>

    suspend fun registrarPesaje(
        puntoId: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String? = null
    ): Result<Pesaje>

    suspend fun corregirPesaje(
        id: Long,
        material: String,
        pesoBruto: Double,
        tara: Double,
        observacion: String? = null
    ): Result<Pesaje>

    suspend fun anularPesaje(id: Long, motivo: String): Result<Unit>

    suspend fun registrarOmision(puntoId: Long, motivo: String): Result<Unit>

    suspend fun cerrarRuta(): Result<Unit>

    suspend fun sincronizar(): Result<ResumenSincronizacion>

    suspend fun limpiarDatosLocales()
}
