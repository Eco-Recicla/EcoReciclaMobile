package pe.upeu.ecorecicla

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.domain.model.Sesion

class AlmacenSeguroAndroid(private val context: Context) : AlmacenSeguro {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "ecorecicla_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback defensivo para entornos restringidos o pruebas
            context.getSharedPreferences("ecorecicla_secure_fallback", Context.MODE_PRIVATE)
        }
    }

    override suspend fun guardarSesion(sesion: Sesion) {
        prefs.edit()
            .putString(KEY_TOKEN, sesion.token)
            .putString(KEY_USUARIO, sesion.usuario)
            .putString(KEY_ROL, sesion.rol)
            .putStringSet(KEY_PRIVILEGIOS, sesion.privilegios)
            .apply()
    }

    override suspend fun obtenerSesion(): Sesion? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val usuario = prefs.getString(KEY_USUARIO, null) ?: return null
        val rol = prefs.getString(KEY_ROL, "OPERARIO") ?: "OPERARIO"
        val privilegios = prefs.getStringSet(KEY_PRIVILEGIOS, emptySet()) ?: emptySet()

        return Sesion(
            token = token,
            usuario = usuario,
            rol = rol,
            privilegios = privilegios
        )
    }

    override suspend fun limpiarSesion() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USUARIO = "usuario_nombre"
        private const val KEY_ROL = "usuario_rol"
        private const val KEY_PRIVILEGIOS = "usuario_privilegios"
    }
}
