package pe.upeu.ecorecicla.presentation.formulario

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.usecase.CorregirPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.RegistrarPesajeUseCase
import kotlin.math.roundToInt

data class FormularioPesajeUiState(
    val material: String = "Plástico",
    val pesoBruto: String = "",
    val tara: String = "",
    val observacion: String = "",
    val pesoNetoCalculado: Double? = null,
    val guardando: Boolean = false,
    val errorMaterial: String? = null,
    val errorPesoBruto: String? = null,
    val errorTara: String? = null,
    val mensajeGeneral: String? = null,
    val guardadoExitoso: Boolean = false,
    val esOffline: Boolean = false
)

class FormularioPesajeViewModel(
    val puntoId: Long,
    val pesajeIdParaEditar: Long? = null,
    private val registrarPesajeUseCase: RegistrarPesajeUseCase,
    private val corregirPesajeUseCase: CorregirPesajeUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(FormularioPesajeUiState())
    val uiState: StateFlow<FormularioPesajeUiState> = _uiState.asStateFlow()

    fun onMaterialChanged(nuevo: String) {
        _uiState.value = _uiState.value.copy(material = nuevo, errorMaterial = null)
    }

    fun onPesoBrutoChanged(nuevo: String) {
        val state = _uiState.value.copy(pesoBruto = nuevo, errorPesoBruto = null)
        _uiState.value = recalcularNeto(state)
    }

    fun onTaraChanged(nueva: String) {
        val state = _uiState.value.copy(tara = nueva, errorTara = null)
        _uiState.value = recalcularNeto(state)
    }

    fun onObservacionChanged(nueva: String) {
        _uiState.value = _uiState.value.copy(observacion = nueva)
    }

    private fun recalcularNeto(state: FormularioPesajeUiState): FormularioPesajeUiState {
        val bruto = state.pesoBruto.toDoubleOrNull()
        val taraVal = state.tara.toDoubleOrNull()
        val neto = if (bruto != null && taraVal != null) {
            val calc = ((bruto - taraVal) * 100).roundToInt() / 100.0
            calc
        } else {
            null
        }
        return state.copy(pesoNetoCalculado = neto)
    }

    fun guardar() {
        val state = _uiState.value
        if (state.guardando) return

        var tieneError = false
        var errBruto: String? = null
        var errTara: String? = null
        var errMat: String? = null

        val brutoVal = state.pesoBruto.toDoubleOrNull()
        if (brutoVal == null || brutoVal <= 0.0) {
            errBruto = "El peso bruto debe ser un número mayor a cero"
            tieneError = true
        }

        val taraVal = state.tara.toDoubleOrNull()
        if (taraVal == null || taraVal < 0.0) {
            errTara = "La tara debe ser un número mayor o igual a cero"
            tieneError = true
        }

        if (brutoVal != null && taraVal != null && brutoVal <= taraVal) {
            errTara = "La tara debe ser menor que el peso bruto para que el neto sea mayor a cero"
            tieneError = true
        }

        if (state.material.isBlank()) {
            errMat = "El tipo de material es obligatorio"
            tieneError = true
        }

        if (tieneError) {
            _uiState.value = state.copy(
                errorPesoBruto = errBruto,
                errorTara = errTara,
                errorMaterial = errMat
            )
            return
        }

        _uiState.value = state.copy(guardando = true, mensajeGeneral = null)

        scope.launch {
            val resultado = if (pesajeIdParaEditar != null) {
                corregirPesajeUseCase(
                    id = pesajeIdParaEditar,
                    material = state.material,
                    pesoBruto = brutoVal!!,
                    tara = taraVal!!,
                    observacion = state.observacion.ifBlank { null }
                )
            } else {
                registrarPesajeUseCase(
                    puntoId = puntoId,
                    material = state.material,
                    pesoBruto = brutoVal!!,
                    tara = taraVal!!,
                    observacion = state.observacion.ifBlank { null }
                )
            }

            resultado.fold(
                onSuccess = { pesaje ->
                    _uiState.value = _uiState.value.copy(
                        guardando = false,
                        guardadoExitoso = true,
                        esOffline = pesaje.pendiente
                    )
                },
                onFailure = { error ->
                    if (error is ErrorApi.Validacion) {
                        _uiState.value = _uiState.value.copy(
                            guardando = false,
                            errorPesoBruto = error.erroresPorCampo["pesoBruto"],
                            errorTara = error.erroresPorCampo["tara"],
                            errorMaterial = error.erroresPorCampo["material"],
                            mensajeGeneral = error.message
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            guardando = false,
                            mensajeGeneral = error.message ?: "Error al guardar el pesaje"
                        )
                    }
                }
            )
        }
    }
}
