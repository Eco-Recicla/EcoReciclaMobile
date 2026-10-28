package pe.upeu.ecorecicla.data.remote

object ApiConfig {
    // Dirección por defecto para emulador Android apuntando al host local en el puerto 8080
    // En dispositivo físico con 'adb reverse tcp:8080 tcp:8080' se puede usar http://127.0.0.1:8080/api/v1
    var baseUrl: String = "http://10.0.2.2:8080/api/v1"
}
