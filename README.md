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

## Clave de la pantalla de Configuración

La pantalla de Configuración (menú lateral) pide una clave, para que un operario no
cambie puesto/servidor sin querer desde el piso. En el repo sólo vive el **hash
SHA-256**, nunca la clave:

- **Local**: copiar `config.properties.example` a `config.properties` (gitignored) y
  completar `CONFIG_PASSWORD_SHA256`. Generar el hash con:
  `printf '%s' 'LA_CLAVE' | sha256sum | cut -d' ' -f1`
- **CI**: el secret de organización `ORG_CONFIG_PASSWORD` guarda la **clave en texto**; el
  workflow calcula su SHA-256 en el runner (paso "Derivar hash de la clave de Configuración")
  y se lo pasa a Gradle. La clave nunca se escribe en el repo ni en el APK, y el hash
  derivado se enmascara en los logs con `::add-mask::`. Si falta el secret, el build falla.
- Si el hash queda **vacío** (sin `config.properties` ni `-P`), la pantalla no pide clave.
  Cómodo en desarrollo, y evita dejar afuera al de planta si un build sale sin el secret.

> Es una barrera contra el toque curioso, no un control de seguridad: una clave embebida
> en un APK siempre es recuperable descompilando, aunque esté hasheada.

## Versiones y publicación (CI)

- **En cada PR a `dev` o `main`** el workflow `apk-pr.yml` (check **Compilar APK**)
  compila y firma el APK con el código de la PR y la versión definitiva, y lo deja
  como artifact. Es obligatorio para mergear (ruleset "Compilar APK obligatorio").
  - PR a `dev`: tiene que estar al día con `dev` (si no, pide "Update branch").
  - PR `dev` → `main`: `main` no puede tener cambios que no estén en `dev`.
- **Al mergear**, `publicar-apk.yml` publica *ese mismo* APK, sin recompilar: tag +
  release en GitHub + repo F-Droid. Antes verifica que el APK sea del último commit
  de la PR, con las mismas etiquetas, la misma base y la versión que corresponde.
- **La versión vive en los tags, no en el código**: cada publicación crea un tag
  anotado `X.Y.Z[-rc.N]` con el mensaje `versionCode=N` sobre el merge commit, y el
  build siguiente parte del tag con el `versionCode` más alto. `versionName`/`versionCode`
  de `app/build.gradle.kts` solo valen para builds locales. Como el workflow no
  pushea commits, no necesita saltear el check obligatorio de las ramas.
- **Versiones**: en `dev` se publican release candidates `X.Y.Z-rc.N`; la PR
  `dev` → `main` publica la estable `X.Y.Z`.
  La etiqueta de la PR (`bugfix` por defecto, `feature`, `breaking`) define a qué
  versión apunta el ciclo; mientras no suba de nivel, cada merge suma 1 al rc.
  Ej. desde 1.1.2: bugfix → `1.1.3-rc.1`, bugfix → `1.1.3-rc.2`, feature → `1.2.0-rc.1`,
  main → `1.2.0`, bugfix → `1.2.1-rc.1`. La lógica está en `.github/scripts/`.
- `versionCode` es siempre el anterior + 1.
- **Tests**: `tests.yml` (check **test**) corre los tests unitarios JVM
  (`./gradlew testDebugUnitTest`) en cada PR a `dev` o `main`. También es obligatorio
  para mergear; si falla, el reporte HTML queda como artifact `reporte-tests`.

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

<!-- CI: verificación final del pipeline (secrets de firma OK). -->
