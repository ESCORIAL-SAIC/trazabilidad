# Trazabilidad — App Android nativa (Kotlin)

Migracion de la app Delphi/FireMonkey a Android nativo. La app NO accede a bases
de datos: consume la API Node (`../trazabilidad-api`).

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Navigation Compose
- Retrofit + OkHttp + kotlinx.serialization (red)
- DataStore Preferences (configuracion local del puesto)
- ZXing (`zxing-android-embedded`) para escaneo de codigos de barras
- Arquitectura MVVM (ViewModel por pantalla, repositorio unico)

## Requisitos

- Android Studio (Koala o superior) / JDK 17
- minSdk 26, targetSdk 34

## Configuracion de la URL de la API

En `app/build.gradle.kts`, campo `API_BASE_URL` (BuildConfig). Por defecto apunta
a la base TEST `http://10.90.99.114:3000/`. Cambiar por el servidor de planta.
`usesCleartextTraffic=true` esta activo para permitir HTTP en la LAN.

## Como abrir/compilar

1. Abrir la carpeta `trazabilidad-android` en Android Studio (genera el wrapper
   y descarga dependencias), o ejecutar `gradle wrapper` y luego `./gradlew assembleDebug`.
2. Conectar un dispositivo Android y ejecutar.

> Nota: el wrapper (`gradlew`, `gradle-wrapper.jar`) lo genera Android Studio al
> abrir el proyecto; aqui se incluye solo `gradle-wrapper.properties`.

## Flujo de arranque

1. **Primer arranque** → pantalla **Configuración del servidor** (URL de la API + planta), con "Probar conexión" (consulta `GET /plantas`).
2. Al continuar se guarda y se marca `inicializado`; las siguientes aperturas van directo al **Login**.
3. La URL/planta se puede cambiar luego desde el botón "Configurar servidor" en el Login, o desde el ícono de Configuración en la pantalla principal.

## Estado de la migracion (pantallas)

| Pantalla | Equivale a (Delphi) | Estado |
|---|---|---|
| ServerSetupScreen | (nuevo) setup previo al login | Implementada |
| LoginScreen | UnitIngreso (doble usuario) | Implementada (Fase 3) |
| ConfiguracionScreen | TabItemConfiguracion | Implementada (Fase 3) |
| ScanScreen | TabItemBlanco (entrada/escaneo) | Implementada (Fase 4) |
| ControladorScreen | TabItemControlador | Implementada (Fase 4) |
| ReparadorScreen | TabItemReparador/V1 | Implementada (Fase 5) |
| EstadoScreen | TabItemEstado | Implementada (Fase 4) |

## Estructura

```
app/src/main/java/com/escorial/trazabilidad/
├── MainActivity.kt
├── data/
│   ├── api/          # Retrofit: TrazabilidadApi, ApiClient, dto/Dtos.kt
│   ├── local/        # ConfiguracionStore (DataStore)
│   └── repo/         # TrazabilidadRepository
├── domain/           # Models.kt (Sesion, ConfiguracionPuesto, tipos)
└── ui/
    ├── theme/  common/  navigation/
    ├── login/  config/  scan/
    └── controlador/  reparador/  estado/
```

<!-- CI: verificación inicial del pipeline de release (dev). -->

<!-- CI: re-verificación del pipeline tras fix de firma JKS. -->
