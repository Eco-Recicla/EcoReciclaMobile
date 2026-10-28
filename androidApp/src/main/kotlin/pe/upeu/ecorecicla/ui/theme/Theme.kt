package pe.upeu.ecorecicla.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VerdePrimario = Color(0xFF1B5E20)
private val VerdeSecundario = Color(0xFF2E7D32)
private val VerdeClaro = Color(0xFF81C784)
private val AmbarAlerta = Color(0xFFF57F17)
private val RojoError = Color(0xFFC62828)
private val FondoClaro = Color(0xFFF9FBE7)
private val FondoOscuro = Color(0xFF121212)

private val LightColorScheme = lightColorScheme(
    primary = VerdePrimario,
    secondary = VerdeSecundario,
    tertiary = VerdeClaro,
    error = RojoError,
    background = Color(0xFFFBFDF8),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = VerdeClaro,
    secondary = VerdeSecundario,
    tertiary = VerdePrimario,
    error = Color(0xFFEF9A9A),
    background = FondoOscuro,
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onError = Color.Black
)

@Composable
fun EcoReciclaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
