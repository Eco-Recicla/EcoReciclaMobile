package pe.upeu.ecorecicla.presentation.sesion

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.usecase.LoginUseCase
import pe.upeu.ecorecicla.domain.usecase.RecuperarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase

data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val contrasenaVisible: Boolean = false,
    val cargando: Boolean = false,
    val mensajeError: String? = null,
    val errorConexion: Boolean = false,
    val sesionExitosa: Sesion? = null
)

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val recuperarSesionUseCase: RecuperarSesionUseCase,
    private val sincronizarUseCase: SincronizarUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        verificarSesionExistente()
    }

    fun verificarSesionExistente() {
        scope.launch {
            val sesion = recuperarSesionUseCase()
            if (sesion != null) {
                _uiState.value = _uiState.value.copy(sesionExitosa = sesion)
            }
        }
    }

    fun onUsuarioChanged(nuevo: String) {
        _uiState.value = _uiState.value.copy(usuario = nuevo, mensajeError = null)
    }

    fun onContrasenaChanged(nueva: String) {
        _uiState.value = _uiState.value.copy(contrasena = nueva, mensajeError = null)
    }

    fun toggleContrasenaVisible() {
        _uiState.value = _uiState.value.copy(contrasenaVisible = !_uiState.value.contrasenaVisible)
    }

    fun iniciarSesion() {
        val state = _uiState.value
        if (state.cargando) return

        if (state.usuario.isBlank()) {
            _uiState.value = state.copy(mensajeError = "Ingrese el nombre de usuario")
            return
        }
        if (state.contrasena.isBlank()) {
            _uiState.value = state.copy(mensajeError = "Ingrese la contraseña")
            return
        }

        _uiState.value = state.copy(cargando = true, mensajeError = null, errorConexion = false)

        scope.launch {
            val resultado = loginUseCase(state.usuario, state.contrasena)
            resultado.fold(
                onSuccess = { sesion ->
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        sesionExitosa = sesion
                    )
                    // Disparar sincronización inicial en segundo plano
                    scope.launch { sincronizarUseCase() }
                },
                onFailure = { error ->
                    val esSinConexion = error is ErrorApi.SinConexion || error is ErrorApi.TiempoAgotado
                    val mensaje = when (error) {
                        is ErrorApi.SinConexion -> "No se pudo conectar al servidor. Verifique su red"
                        is ErrorApi.TiempoAgotado -> "Tiempo de espera agotado al conectar"
                        is ErrorApi.NoAutorizado -> "Usuario o contraseña incorrectos"
                        else -> error.message ?: "Error al iniciar sesión"
                    }
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        mensajeError = mensaje,
                        errorConexion = esSinConexion
                    )
                }
            )
        }
    }

    fun limpiarEstadoLogin() {
        _uiState.value = LoginUiState()
    }
}
