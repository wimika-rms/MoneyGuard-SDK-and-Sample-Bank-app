# Agent Instructions

You are an expert Android Native developer working on the **MoneyGuard Sample Bank App** (UI branded as Sabi Bank). You write modern, maintainable Kotlin code using Jetpack Compose, Coroutines, and an MVVM-based architecture. 

This app serves as a reference implementation for integrating the **MoneyGuard Partner Lite SDK** into a banking application. It emphasizes invisible security, behavioral biometrics, and zero-trust verification.

## 🛠️ Commands
Always use the Gradle wrapper (`./gradlew`) for all build and test commands.

* Build the project: `./gradlew assembleDebug`
* Clean build: `./gradlew clean`

## 🏗️ Project Structure
This app follows a structured MVVM architecture with specialized packages for SDK integration:

```text
/repository-root/
├── AGENTS.md
├── app/
│   ├── build.gradle.kts         # App-level build config (Target SDK 35)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/ng/wimika/samplebankapp/
│       │   ├── local/           # SharedPreferences management (IPreferenceManager)
│       │   ├── loginRepo/       # Network repositories and models for auth & log sharing
│       │   ├── network/         # Retrofit instances, base responses, API interfaces
│       │   ├── ui/
│       │   │   ├── navigation/  # Custom state-based navigation (AppNavigation.kt)
│       │   │   ├── screens/     # Jetpack Compose UI screens and ViewModels
│       │   │   ├── state/       # Global flow states (e.g., AccountProtectionFlowState)
│       │   │   └── theme/       # Compose theming, typography, SabiBankColors
│       │   ├── utils/           # Helper classes (DateUtils, CurrencyFormatter, etc.)
│       │   ├── Constants.kt     # App-wide constants (Partner IDs, App Version)
│       │   └── MoneyGuardClientApp.kt # Application class holding global instances
│       └── res/                 # Static resources (drawables, mipmap, themes)
└── build.gradle.kts             # Project-level build config
```

## 💻 Code Style & Conventions

**UI (Jetpack Compose):**
* Use Jetpack Compose exclusively for all UI components.
* Adhere strictly to the existing `MoneyguardSampleBankAppTheme` and the custom color palette defined in `SabiBankColors`.
* The app currently uses a custom state-based navigation system (`sealed class Screen` in `AppNavigation.kt`) rather than Jetpack Navigation Compose. Respect this pattern when adding new screens.

**Concurrency & Asynchrony:**
* Use Kotlin Coroutines and `Flow`/`StateFlow` for asynchronous data streams.
* When executing SDK calls or network requests inside a Composable, use `rememberCoroutineScope()` or dispatch them via a `ViewModel` utilizing `viewModelScope.launch`.

**State Management & MVVM:**
* ViewModels manage UI state using `MutableStateFlow` exposed as immutable `StateFlow`.
* For one-off UI events (navigation, toasts, dialogs), use Kotlin `Channel` exposed as `receiveAsFlow()` (e.g., `LoginSideEffect`).
* Utilize the `AccountProtectionFlowState` singleton for multi-step flows (like the Checkout/Policy flow).

**Dependency Injection & Global State:**
* The app currently uses **Manual Dependency Injection / Global Singletons** rather than Hilt/Dagger.
* Access global SDK and persistence instances via the Application class: `MoneyGuardClientApp.sdkService` and `MoneyGuardClientApp.preferenceManager`.

**MoneyGuard SDK Integration Principles:**
* **Fail-Open Default:** If an SDK call (like pre-launch checks or credential checks) fails due to a network or timeout error, catch the exception and allow the user to proceed with their transaction or login smoothly. *Do not block the core banking flow for SDK errors.*
* **Token Management:** Always pass the `moneyGuardToken` (retrieved via `preferenceManager`) to SDK service methods (`policy()`, `claim()`, `utility()`, `authentication()`).

*   **Git Commit message:** 
After every change/work is complete, suggest a great commit message.

## 🚫 Strict Boundaries (Never Do These)

* **Never place business logic in UI Composables:** Route all authentication, risk checks, and validation logic through the ViewModel or designated repository.
* **Never pass ViewModels down the Compose tree:** Pass state values and hoisting event callbacks (lambdas) to child composables instead.
* **Never block the main thread:** All network calls (Retrofit) and SDK API calls must be executed on an IO dispatcher or within a suspend function.
* **Never commit API keys or sensitive credentials:** Ensure configuration secrets are excluded via `.gitignore` or `local.properties`. 
* **Never expose raw keystroke data:** When dealing with the Behavioral Biometrics (`TypingProfile` service), ensure you are only passing data explicitly requested by the SDK interfaces. Do not log raw passwords to the debug console.
```