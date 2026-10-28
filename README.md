# EcoRecicla Mobile — EcoRutas

Aplicación móvil Android desarrollada en **Kotlin Multiplatform (KMP)** con **Jetpack Compose**, **Ktor Client**, **SQLDelight**, **Koin** y arquitectura **Offline-First**, diseñada para el registro y pesaje de rutas de reciclaje en condiciones de baja o nula cobertura celular.

---

## 1. Instrucciones de Compilación y Ejecución

### Requisitos
- **JDK:** Java 21 LTS (`JAVA_HOME` apuntando a JDK 21).
- **Android SDK:** API 34 (`compileSdk = 34`, `minSdk = 26`).
- **Gradle Wrapper:** Incluido en el repositorio (`gradlew` / `gradlew.bat`).
- **Backend EcoAPI:** Ejecutándose en `http://localhost:8080/api/v1`.

### Comandos Principales (desde la raíz de `EcoReciclaMobile`)

**1. Ejecutar las pruebas unitarias y de integración de `commonTest`:**
```powershell
.\gradlew.bat :shared:allTests
```
*O específicamente:*
```powershell
.\gradlew.bat :shared:testDebugUnitTest
```

**2. Compilar el APK de depuración:**
```powershell
.\gradlew.bat :androidApp:assembleDebug
```
El APK se genera en:
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`

**3. Instalar y ejecutar en un dispositivo o emulador conectado:**
```powershell
.\gradlew.bat :androidApp:installDebug
```
*O mediante adb directo:*
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n pe.upeu.ecorecicla/.MainActivity
```

---

## 2. Configuración de Red para Emulador y Dispositivo Físico

La URL base del backend se centraliza en `pe.upeu.ecorecicla.data.remote.ApiConfig`:

- **Emulador Android Oficial:**
  Usa por defecto `http://10.0.2.2:8080/api/v1` (dirección especial del emulador para acceder al `localhost` del host de desarrollo).

- **Dispositivo Físico conectado por USB:**
  Ejecutar el reenvío de puertos mediante `adb reverse`:
  ```powershell
  adb reverse tcp:8080 tcp:8080
  ```
  Permite al teléfono físico conectarse mediante `http://127.0.0.1:8080/api/v1`.

- **Red Local (Wi-Fi):**
  Configurar la IP LAN del PC de desarrollo (ejemplo: `http://192.168.1.100:8080/api/v1`).

---

## 3. Arquitectura y Recorrido de un Registro

### Estructura de Paquetes
```
shared/src/commonMain/kotlin/pe/upeu/ecorecicla/
├── domain/
│   ├── model/ (PuntoAcopio, EstadoPesaje, Pesaje, Sesion, ErrorApi, ResumenSincronizacion)
│   ├── repository/ (EcoReciclaRepository, SesionRepository)
│   └── usecase/ (LoginUseCase, ObtenerPuntosRutaUseCase, RegistrarPesajeUseCase, SincronizarUseCase, etc.)
├── data/
│   ├── remote/ (ClienteHttp, ApiConfig, MapeoErrores, dto/)
│   ├── local/ (AlmacenSeguro, BaseLocal, esquema EcoReciclaDatabase.sq)
│   └── repository/ (EcoReciclaRepositoryImpl, SesionRepositoryImpl)
├── presentation/ (LoginViewModel, RutaViewModel, DetallePuntoViewModel, FormularioPesajeViewModel, PerfilViewModel)
└── di/ (redModule, dataModule, domainModule, presentationModule)

androidApp/src/main/kotlin/pe/upeu/ecorecicla/
├── AlmacenSeguroAndroid.kt (EncryptedSharedPreferences con Android Keystore)
├── BaseLocalAndroid.kt (AndroidSqliteDriver para SQLDelight)
├── PlatformModule.android.kt (inyección de dependencias de Android)
├── MainActivity.kt (Compose Navigation y NetworkCallback para auto-sincronización)
└── ui/ (LoginScreen, RutaScreen, DetallePuntoScreen, FormularioPesajeScreen, PerfilScreen, AdminScreen)
```

### Recorrido de un Registro de Pesaje:
1. **Compose UI (`FormularioPesajeScreen`):** El usuario ingresa peso bruto y tara. La UI muestra reactivamente el cálculo de peso neto estimado.
2. **ViewModel (`FormularioPesajeViewModel`):** Valida que bruto > tara y tara >= 0. Delega a `RegistrarPesajeUseCase`.
3. **Repositorio Offline-First (`EcoReciclaRepositoryImpl`):**
   - Genera transaccionalmente un ID negativo provisional mediante `COALESCE(MIN(id), 0) - 1`.
   - Inserta la fila en SQLite con `pendiente = 1`.
   - La pantalla principal (`RutaScreen`) observa la tabla por `Flow` y actualiza la lista inmediatamente.
4. **Envío / Sincronización:**
   - Si hay red, envía `PesajeRequestDto` al endpoint `POST /api/v1/pesajes`.
   - El servidor calcula el `pesoNeto` definitivo y responde con `201 Created` y el ID positivo definitivo.
   - En una transacción SQLite, se reemplaza el ID negativo provisional por el ID definitivo y se limpia `pendiente = 0`.
   - Si no hay red (Modo Avión), el pesaje permanece persistido localmente con su ID negativo hasta la próxima sincronización.

---

## 4. Estrategia Offline-First y Almacenamiento Seguro

- **Almacenamiento Seguro (`AlmacenSeguroAndroid`):**
  Respaldado por el Android Keystore mediante `EncryptedSharedPreferences` (AES-256 GCM para valores y AES-256 SIV para claves). Se deshabilita `allowBackup="false"` en el manifiesto para evitar copias inseguras de claves del Keystore.
- **Sincronización:**
  1. Primero sube los pendientes en orden de registro (`POST /pesajes`).
  2. Confirma respuestas y sustituye IDs en SQLite dentro de transacciones.
  3. Descarga la ruta del día actualizada y los pesajes de los puntos.
  4. Actualiza SQLite sin sobrescribir registros pendientes locales.
  5. Protegido con `Mutex` anti-concurrencia.

---

## 5. Resultados de Pruebas Automatizadas

Suite ejecutada con `./gradlew.bat :shared:allTests`:
- `test1 Conversion de DTO a Dominio`: APROBADO.
- `test2 Traduccion de Errores HTTP a ErrorApi`: APROBADO.
- `test3 Registro sin conexion queda guardado con ID negativo y marca pendiente`: APROBADO.
- `test4 Orden de sincronizacion envio antes de descarga y sustitucion de ID provisional`: APROBADO.
