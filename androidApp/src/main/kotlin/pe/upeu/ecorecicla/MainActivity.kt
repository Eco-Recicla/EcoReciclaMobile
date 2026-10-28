package pe.upeu.ecorecicla

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoViewModel
import pe.upeu.ecorecicla.presentation.formulario.FormularioPesajeViewModel
import pe.upeu.ecorecicla.presentation.listado.RutaViewModel
import pe.upeu.ecorecicla.presentation.perfil.PerfilViewModel
import pe.upeu.ecorecicla.presentation.sesion.LoginViewModel
import pe.upeu.ecorecicla.ui.*
import pe.upeu.ecorecicla.ui.theme.EcoReciclaTheme

class MainActivity : ComponentActivity() {

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        iniciarMonitoreoConectividad()

        setContent {
            EcoReciclaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EcoReciclaNavegacion()
                }
            }
        }
    }

    private fun iniciarMonitoreoConectividad() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Al recuperar la conexión a internet, disparar auto-sincronización si hay sesión
                val sincronizarUseCase = org.koin.java.KoinJavaComponent.get<SincronizarUseCase>(SincronizarUseCase::class.java)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        sincronizarUseCase()
                    } catch (_: Exception) {}
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun EcoReciclaNavegacion() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            val viewModel: LoginViewModel = getViewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginExitoso = { sesion ->
                    navController.navigate("ruta") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("ruta") {
            val viewModel: RutaViewModel = getViewModel()
            RutaScreen(
                viewModel = viewModel,
                onPuntoClick = { puntoId ->
                    navController.navigate("detalle/$puntoId")
                },
                onPerfilClick = {
                    navController.navigate("perfil")
                },
                onAdminClick = {
                    navController.navigate("admin")
                }
            )
        }

        composable(
            route = "detalle/{puntoId}",
            arguments = listOf(navArgument("puntoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val puntoId = backStackEntry.arguments?.getLong("puntoId") ?: 0L
            val viewModel: DetallePuntoViewModel = getViewModel { parametersOf(puntoId) }
            DetallePuntoScreen(
                puntoId = puntoId,
                viewModel = viewModel,
                onRegistrarPesajeClick = {
                    navController.navigate("formulario/$puntoId")
                },
                onEditarPesajeClick = { pesajeId ->
                    navController.navigate("formulario/$puntoId?pesajeId=$pesajeId")
                },
                onVolverClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "formulario/{puntoId}?pesajeId={pesajeId}",
            arguments = listOf(
                navArgument("puntoId") { type = NavType.LongType },
                navArgument("pesajeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val puntoId = backStackEntry.arguments?.getLong("puntoId") ?: 0L
            val pesajeIdStr = backStackEntry.arguments?.getString("pesajeId")
            val pesajeId = pesajeIdStr?.toLongOrNull()

            val viewModel: FormularioPesajeViewModel = getViewModel { parametersOf(puntoId, pesajeId) }
            FormularioPesajeScreen(
                puntoId = puntoId,
                pesajeIdParaEditar = pesajeId,
                viewModel = viewModel,
                onGuardadoExitoso = {
                    navController.popBackStack()
                },
                onVolverClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("perfil") {
            val viewModel: PerfilViewModel = getViewModel()
            PerfilScreen(
                viewModel = viewModel,
                onLogoutExitoso = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onVolverClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("admin") {
            AdminScreen(
                onVolverClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
