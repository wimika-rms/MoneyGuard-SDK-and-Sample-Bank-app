# MoneyGuard Partner Lite SDK for Android

The MoneyGuard Partner Lite SDK is an Android library that enables seamless integration of MoneyGuard protection directly into your bank's Android application. This SDK operates as a secure gateway—handling telemetry, continuous authentication, risk assessment, and real-time transaction security—while handing off complex insurance management tasks to the standalone MoneyGuard application.

To accelerate your integration, this repository includes a fully functional **Sample Bank App** (built with Jetpack Compose) demonstrating every SDK feature in practice.

---

## Architecture & Performance Guarantees

We recognize that banking applications demand the highest standards of performance, security, and user privacy. The MoneyGuard SDK is designed with a **"Zero-Touch Integration Strategy"**:

- **Lightweight Footprint:** This is a "Lite SDK" specifically engineered for background telemetry, device binding, and real-time risk decisions without bloating your app size.
- **Zero Latency Impact:** All risk analysis, pre-launch checks, and transaction monitoring are processed asynchronously with "fail-open" default capabilities. MoneyGuard will *never* block your core transaction flows if network degradation occurs.
- **Privacy by Design:** Our Behavioral Biometrics (Typing Profile) captures strictly mathematical metadata (e.g., keystroke flight time, touch pressure). **It does not capture, store, or transmit raw PII or typed text**, acting fundamentally differently from a keylogger.

---

## Requirements
- Android API level 21 or higher
- Kotlin 1.5.0 or higher
- AndroidX libraries

## Installation

Add the following dependencies to your app's `build.gradle` file:

```gradle
dependencies {
    // MoneyGuard SDK modules
    implementation(files("libs/moneyguard-sdk-release.aar"))
    implementation(files("libs/moneyguard-sdk-commons-release.aar"))
    implementation(files("libs/moneyguard-sdk-auth-release.aar"))

    // Required External Dependencies
    implementation(libs.okhttp3)
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.okhttp3.logging)
    implementation(libs.gson.converter)
    implementation(libs.android.joda)
}
```

### Required Manifest Permissions

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<!-- Required for Typing Profile overlay capabilities -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.TYPE_APPLICATION_OVERLAY"/>
```

### Application Configuration

Initialize the SDK in the `onCreate()` method of your custom `Application` class. This requires passing the application `Context`.

```kotlin
class YourBankApplication : Application() {
    companion object {
        var sdkService: MoneyGuardSdkService? = null
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize MoneyGuard SDK
        sdkService = MoneyGuardSdk.initialize(this)
    }
}
```
*💡 See `MoneyGuardClientApp.kt` in the Sample App for an implementation example.*

---

## Authentication & Session Management

**Critical Concept:** The SDK relies on a dual-token system. First, your app authenticates the user as normal and retrieves a `partnerSessionToken`. You then pass this token to the SDK to register the session and receive a distinct **MoneyGuard Token**, which is required for all subsequent SDK API calls.

### 1. Registering the Session

Once your user logs into your bank app, exchange your session token for a MoneyGuard token:

```kotlin
// 1. Get the SDK instance
val moneyGuardAuth = sdkService.authentication()

// 2. Register the session using Kotlin Flows
lifecycleScope.launch {
    moneyGuardAuth.register(partnerBankId = 101, partnerSessionToken = "your_bank_session_token")
        .collect { result ->
            when (result) {
                is MoneyGuardResult.Loading -> { /* Show loading state */ }
                is MoneyGuardResult.Success -> {
                    val sessionResponse = result.data
                    val moneyGuardToken = sessionResponse.token // 🔑 Save this locally!
                    val hasActivePolicy = sessionResponse.hasActivePolicy
                    
                    // Save token securely (e.g., EncryptedSharedPreferences)
                    preferenceManager.saveMoneyGuardToken(moneyGuardToken)
                }
                is MoneyGuardResult.Failure -> {
                    // Fail-open: Log the error and proceed with bank login safely
                    Log.e("MoneyGuard", "Registration failed: ${result.error.message}")
                }
            }
        }
}
```
*💡 See `LoginViewModel.kt` in the Sample App for a robust implementation handling state, errors, and fail-open logic.*

### 2. Terminating the Session
When the user logs out of your banking app, ensure you also terminate the MoneyGuard session to cleanly sever the IPC (Inter-Process Communication) bind.

```kotlin
sdkService.authentication().logout()
```

---

## Standalone App Handoff (The Gateway Approach)

To prevent your bank app from becoming cluttered, policy creation, claims management, and deep configurations are managed in the standalone MoneyGuard app. Your app acts as the gateway.

Use the `Utility` service to route users intelligently:

```kotlin
val utility = sdkService.utility()

fun handleProtectAccountClick() {
    if (utility.isMoneyGuardInstalled()) {
        // App is installed, launch it directly via IPC/Deep link
        val launched = utility.launchMoneyGuardApp()
        if (!launched) {
            // Fallback error handling
        }
    } else {
        // App is not installed, route user to Play Store
        utility.launchAppInstallation()
    }
}
```

You can also check the real-time status of the user's MoneyGuard protection using the token obtained during authentication:

```kotlin
val status: MoneyGuardAppStatus = utility.checkMoneyguardStatus(moneyGuardToken)
// Returns enums like: Active, InActive, NoPolicyAppInstalled, ValidPolicyAppNotInstalled
```

---

## Pre-launch Checks (Risk Assessment)

Run pre-launch checks immediately upon application start or after login to ensure the device environment is secure (checking for root, debug mode, unencrypted Wi-Fi, etc.).

```kotlin
lifecycleScope.launch {
    val startupRisk = sdkService.prelaunch().startup()
    
    when (startupRisk.preLaunchVerdict.decision) {
        PreLaunchDecision.Launch -> {
            // Environment is safe, proceed normally
        }
        PreLaunchDecision.LaunchWithWarning -> {
            // Show warning (e.g., "Unsecured Wi-Fi detected"), but allow login
            val reasons = startupRisk.preLaunchVerdict.reasons
        }
        PreLaunchDecision.DoNotLaunch -> {
            // Critical risk (e.g., Device Rooted, DNS Spoofed). Block login.
            showCriticalSecurityError()
        }
    }
}
```

---

## Credential Checking

Ensure the user is not logging in using a password compromised in known data breaches. 

```kotlin
val credential = Credential(
    username = "user@example.com",
    passwordStartingCharactersHash = "hashedPasswordFragment", // Never send raw passwords
    hashAlgorithm = HashAlgorithm.SHA256,
    domain = "yourbank.com"
)

lifecycleScope.launch {
    sdkService.authentication().credentialCheck(moneyGuardToken, credential)
        .collect { result ->
            if (result is MoneyGuardResult.Success) {
                if (result.data.status == RiskStatus.RISK_STATUS_UNSAFE) {
                    // Force user to reset password or trigger 2FA
                }
            }
        }
}
```

---

## Transaction Security

Before authorizing a debit transfer, run it through the `TransactionCheck` service to evaluate real-time behavioral and geographic anomalies.

```kotlin
val transaction = DebitTransaction(
    sourceAccountNumber = "1234567890",
    amount = 50000.0,
    memo = "Rent Payment",
    destinationBank = "057",
    destinationAccountNumber = "0987654321"
)

lifecycleScope.launch {
    try {
        val checkResult = sdkService.transactionCheck()
            .checkDebitTransaction(moneyGuardToken, transaction)
            
        when (checkResult.status) {
            RiskStatus.RISK_STATUS_SAFE -> { /* Process Transfer */ }
            RiskStatus.RISK_STATUS_WARN -> { /* Show warning / request confirmation */ }
            RiskStatus.RISK_STATUS_UNSAFE_LOCATION,
            RiskStatus.RISK_STATUS_UNSAFE_CREDENTIALS,
            RiskStatus.RISK_STATUS_UNSAFE -> {
                // High risk detected. Trigger Step-Up Authentication (OTP/FaceID)
            }
        }
    } catch (e: Exception) {
        // Fail-open: If the API times out, process the transfer to prevent friction
    }
}
```

---

## Biometric Authentication (Typing Profile)

The `TypingProfile` service captures behavioral typing patterns as a biometric identifier. Note that this requires `SYSTEM_ALERT_WINDOW` permission to accurately capture telemetry data.

### Integrating with XML Layouts
If you are using legacy Android Views:
```kotlin
val targets = intArrayOf(R.id.edittext_username, R.id.edittext_password)
lifecycleScope.launch {
    sdkService.getTypingProfile().startService(this@LoginActivity, targets)
}
```

### Integrating with Jetpack Compose (Modern UI)
Jetpack Compose does not natively use integer View IDs. To integrate the SDK into a Compose screen, use an `AndroidView` wrapper around a traditional `EditText` and assign it a static ID.

```kotlin
// 1. Define a static ID
private const val TYPING_PROFILE_INPUT_ID = 1001

@Composable
fun LoginScreen() {
    val typingProfileService = MoneyGuardClientApp.sdkService?.getTypingProfile()
    val context = LocalContext.current

    // 2. Start the service (Ensure overlay permissions are granted first!)
    LaunchedEffect(Unit) {
        typingProfileService?.startService(context as Activity, intArrayOf(TYPING_PROFILE_INPUT_ID))
    }

    // 3. Wrap standard EditText using AndroidView
    AndroidView(
        factory = { ctx ->
            EditText(ctx).apply {
                id = TYPING_PROFILE_INPUT_ID
                hint = "Enter Username"
                inputType = InputType.TYPE_CLASS_TEXT
                
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        // Update your Compose state here
                    }
                })
            }
        },
        modifier = Modifier.fillMaxWidth().height(56.dp)
    )
}
```
*💡 See `TypingPatternEnrollmentScreen.kt` in the Sample App for a full Jetpack Compose integration, including how to prompt users for Overlay permissions.*

---

## Policy & Claims Management

While complex claims and policy configurations belong in the standalone app, the SDK allows you to fetch summaries and submit basic claims directly from the banking interface.

### Submitting a Claim

```kotlin
val claim = Claim(
    accountId = 1234567890L,
    lossDate = Date(),
    nameOfIncident = "Fraudulent Transfer",
    lossAmount = 50000.0,
    statement = "I noticed an unauthorized transfer..."
)

// Prepare attachments (Max 5 files, 10MB each)
val imagePart = MultipartBody.Part.createFormData(
    "file", imageFile.name, imageFile.asRequestBody("image/*".toMediaType())
)

lifecycleScope.launch {
    val response = sdkService.claim().submitClaim(moneyGuardToken, claim, listOf(imagePart))
    if (response.success) {
        // Claim submitted successfully
    }
}
```
*💡 See `ClaimsListScreen.kt` and `SubmitClaimScreen.kt` in the Sample App for complete implementations using Jetpack Compose.*

---

## Exploring the Sample App

We strongly encourage developers to review the `moneyguard-sample-bank-app` included in this repository. It serves as the ultimate source of truth for implementation best practices, featuring:
- **Clean Architecture** patterns.
- Proper **Fail-Open error handling**.
- **Jetpack Compose** wrapper implementations.
- Complete **Overlay Permission** request flows.
- Real-world **Token management** and secure preference storage.

## Support

For technical support, engineering questions, or to request integration assistance, please contact:
- **Email:** tech@wimika.ng


## License

This SDK is proprietary software owned by Wimika RMS. All rights reserved.
