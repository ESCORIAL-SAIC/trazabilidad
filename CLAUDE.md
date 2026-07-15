# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Trazabilidad is a native Android (Kotlin + Jetpack Compose) rewrite of a legacy
Delphi/FireMonkey plant-floor traceability app. It has **no local database** —
all data comes from a separate Node API (`../trazabilidad-api`, sibling repo,
not part of this workspace). Screens are a 1:1 migration of the old Delphi
tabs/forms; when working on a screen it can help to know which Delphi form it
replaces (see the table in README.md).

Language: code, comments, and commit-worthy strings are in **Spanish**
(matches the original Delphi app's domain vocabulary — `puesto`, `etiqueta`,
`falla`, `planta`, etc.). Keep new code consistent with this.

## Commands

Build/lint/test (Windows, from repo root):

```
.\gradlew.bat assembleDebug        # debug APK
.\gradlew.bat assembleRelease      # release APK (needs keystore.properties, see below)
.\gradlew.bat test                 # all JVM unit tests (app/src/test)
.\gradlew.bat test --tests "com.escorial.trazabilidad.ui.config.ConfiguracionViewModelTest"
.\gradlew.bat test --tests "*ConfiguracionViewModelTest.confirmar*"   # single test method (pattern match)
.\gradlew.bat lint
```

On Unix-like shells (Git Bash) use `./gradlew` instead of `.\gradlew.bat`.

There are no instrumented (`androidTest`) tests currently — all tests are JVM
unit tests under `app/src/test`, run with JUnit4 + MockK + kotlinx-coroutines-test.

Release signing: copy `keystore.properties.example` (if present) to
`keystore.properties` and fill in `storeFile`/`storePassword`/`keyAlias`/`keyPassword`.
This file is untracked/local-only; without it, release builds compile unsigned
(`app/build.gradle.kts` checks `keystoreProps.exists()`).

## Architecture

MVVM, single shared repository, no DI framework (plain constructor wiring).

```
app/src/main/java/com/escorial/trazabilidad/
├── MainActivity.kt          # loads ConfiguracionStore, sets ApiConfig, picks start route
├── data/
│   ├── api/
│   │   ├── TrazabilidadApi.kt   # Retrofit interface — one method per backend endpoint
│   │   ├── ApiClient.kt         # builds/caches the Retrofit instance for current baseUrl
│   │   ├── ApiConfig.kt         # mutable @Volatile globals: current baseUrl + planta
│   │   └── dto/Dtos.kt          # all @Serializable request/response DTOs, one file
│   ├── local/ConfiguracionStore.kt  # DataStore Preferences (replaces old Configuracion.db)
│   └── repo/TrazabilidadRepository.kt  # single repo, thin pass-through to ApiClient.api()
├── domain/
│   ├── Models.kt   # ConfiguracionPuesto (persisted puesto config), Sesion/SesionActual
│   └── Flujo.kt    # FlujoActual — in-memory hand-off of the last /scan/resolver response
└── ui/
    ├── navigation/  # Routes (string constants) + NavGraph (NavHost wiring)
    ├── theme/, common/   # shared Compose bits: AppBars, BarcodeScanner, CampoPickeo, ColorUtils
    └── <feature>/   # login/, config/, scan/, controlador/, reparador/, estado/, setup/
                      # each has ScreenX.kt (Compose UI) + XViewModel.kt (state + logic)
```

Key patterns to preserve when adding/editing code:

- **Network config is runtime-mutable, not build-time.** `ApiConfig.baseUrl` /
  `ApiConfig.planta` are `@Volatile` globals updated from `ConfiguracionStore`
  at startup and whenever the user changes server settings in
  `ConfiguracionScreen`. `ApiClient.api()` rebuilds the Retrofit client
  whenever `baseUrl` changes and injects `X-Planta` on every request via an
  OkHttp interceptor reading `ApiConfig.planta` dynamically — never construct
  Retrofit/OkHttp directly elsewhere.
- **`TrazabilidadRepository`** is the only class that talks to `ApiClient`.
  ViewModels depend on the repository (constructor-injected with a default
  `= TrazabilidadRepository()`, overridable in tests), never on `ApiClient`
  or `TrazabilidadApi` directly.
- **Cross-screen state is passed via singletons, not nav arguments**:
  `SesionActual.sesion` (logged-in operarios, set by `LoginViewModel`) and
  `FlujoActual.resolver` (last `/scan/resolver` response, set by
  `ScanViewModel`, consumed by `ControladorScreen`/`ReparadorScreen`/`EstadoScreen`
  depending on the `accion` field). These are `@Volatile` mutable globals by
  design (mirrors the single-activity, single-operator-session nature of the
  kiosk-style app) — don't refactor to a DI-scoped holder without discussing it.
- **ViewModel shape**: `MutableStateFlow<XUiState>` exposed as `StateFlow`,
  one immutable state data class per screen, mutating via `.copy()`. Screens
  that need `Application`/`Context` (e.g. to build a `ConfiguracionStore`) use
  `AndroidViewModel`; others use plain `ViewModel`. Network calls run in
  `viewModelScope.launch { try { ... } catch (e: Exception) { ... } }`,
  turning failures into a Spanish user-facing `error` message in the state
  rather than propagating exceptions.
- **DTOs mirror backend SQL view names/columns** — e.g. `PuestoDto` fields
  come from `VP_MENUFALLAS_PUESTOCONTROL_V1`, `EmpleadoDto` from
  `VP_APLICACIONES_EMPLEADO`. Most DTO fields are nullable/defaulted because
  the Node API's shape varies by endpoint state; the Json parser is
  configured with `ignoreUnknownKeys = true` and `explicitNulls = false`.
  When adding a field, check the actual API response shape rather than
  assuming non-null.
- **Resolver-driven flow**: `POST /scan/resolver` (`ScanViewModel.buscar`)
  returns an `accion` string (`CONTROLADOR` / `REPARADOR` / `ESTADO` / etc.)
  that both names the next Compose route and is stashed in
  `FlujoActual.resolver` for that screen to read. Special-cased actions
  `IGNORAR` and `ETIQUETA_INVALIDA` don't navigate.
- **Testing ViewModels**: see
  `app/src/test/java/.../ConfiguracionViewModelTest.kt` for the established
  pattern — MockK-mock the repo and store (don't construct a real
  `ConfiguracionStore`, it requires an Android `Context`), drive
  `Dispatchers.Main` with a `MainDispatcherRule` (`UnconfinedTestDispatcher`)
  so `viewModelScope.launch` runs synchronously in the JVM test, and assert on
  `viewModel.state.value` directly.

## Notes

- `minSdk 26`, `targetSdk 34`/`compileSdk 34`, JVM target 17.
- `usesCleartextTraffic="true"` is intentional — the API is plain HTTP on the
  plant LAN (default `API_BASE_URL` in `app/build.gradle.kts` is a test IP;
  real deployments change server URL from within the app's setup/config
  screens, not by rebuilding).
- Barcode scanning uses `zxing-android-embedded` (`ui/common/BarcodeScanner.kt`,
  `rememberEscaner`) — deliberately the same scan engine/format set as the
  Delphi app for parity with existing printed labels.
- No dependency injection framework; keep new screens consistent with the
  existing manual-construction style (ViewModel factories via
  `viewModel { XViewModel(...) }` at the call site, not a DI graph).
