# MoneyGuard Partner SDK for Android

<!-- markdownlint-disable MD013 -->

This repository contains the Sabi Bank reference application for integrating the MoneyGuard Partner SDK into an Android banking app. The guide describes the current B2B2C partner-bank contract: bank-owned authentication and customer data, pseudonymous MoneyGuard sessions, optional host-assisted protection through the standalone MoneyGuard app, server-configured transaction decisions, policy enrolment, and claims.

> **Environment status:** MoneyGuard is currently in development and controlled testing. The shared service is a **staging** deployment, the Sabi Bank API is a simulator, and the standalone app is distributed through Google Play's internal test track. Do not treat the sample endpoints, credentials, signing identities, test OTP, or artifacts as production-ready.

The reference implementation lives in:

- [`MoneyGuardClientApp.kt`](app/src/main/java/ng/wimika/samplebankapp/MoneyGuardClientApp.kt) — one-time SDK initialization
- [`LoginViewModel.kt`](app/src/main/java/ng/wimika/samplebankapp/ui/screens/Login/LoginViewModel.kt) — login, location-aware registration, challenge handling, and degraded states
- [`CheckDebitScreen.kt`](app/src/main/java/ng/wimika/samplebankapp/ui/screens/CheckDebitScreen.kt) — verdict-first transfer screening and bank OTP step-up
- [`PreferenceManager.kt`](app/src/main/java/ng/wimika/samplebankapp/local/PreferenceManager.kt) — encrypted sample storage
- [`app/build.gradle.kts`](app/build.gradle.kts) — AAR and runtime dependencies

## 1. Integration at a glance

A complete integration has three coordinated parts. Changing only the PartnerCode in the sample app is not sufficient.

| Owner | Required work |
| --- | --- |
| Partner Android team | Add all three SDK AARs, initialize once, register after bank login, handle every session state, screen transfers, call SDK logout, and ship a release build that passes R8 testing. |
| Partner backend team | Authenticate the customer, issue a stable opaque bank-user reference, expose the approved server-to-server verification/account/profile interfaces, and verify bank OTP before issuing single-use step-up proof. |
| MoneyGuard onboarding team | Create the partner tenant and directory entry, configure plans/risk rules/content, add the bank adapter and secrets, and authorize every Android package/signing-certificate pair. |

The normal login path is:

```text
Bank app -> Bank login API -> stable BankUserReference
Bank app -> Partner SDK register(PartnerCode, BankUserReference, fresh location)
Partner SDK -> MoneyGuard host over authenticated IPC, when available
             or MoneyGuard service in PartnerOnly mode
MoneyGuard service -> Bank server-to-server session verification
MoneyGuard service -> short-lived partner-api JWT
Partner SDK -> Bank app SessionResponse
```

The standalone MoneyGuard app improves device telemetry and protection but is not required for a PartnerOnly session. Its absence and its temporary failure are distinct states and must remain visible to the bank app.

## 2. Obtain the onboarding package first

Before writing app code, obtain the following from MoneyGuard:

- the generated external **PartnerCode** for the bank;
- one mutually compatible release of all three AARs;
- a checksum manifest for those AARs;
- the target staging environment and certification test identities;
- confirmation that the bank's base URL and server-to-server API key are configured;
- confirmation that the partner, bank-directory entry, plans, fraud rules, and risk factors are active;
- confirmation that the Android application ID and signing-certificate SHA-256 are authorized;
- the supported standalone MoneyGuard app version for HostAssisted testing.

Record debug, staging, release, and Google Play App Signing identities separately. HostAssisted authorization uses the calling package and signing certificate derived from Android Binder identity; a correct PartnerCode does not authorize an unknown APK.

Print an APK's certificate details with Android build tools:

```bash
apksigner verify --print-certs path/to/your-bank.apk
```

Give MoneyGuard the package name, certificate SHA-256, partner environment, and whether the certificate is local, CI, or Play App Signing. Never solve `caller_not_authorised` by silently using PartnerOnly registration.

## 3. Identifier and data-ownership contract

Keep these values separate:

| Value | Created and owned by | Purpose | Storage/logging rule |
| --- | --- | --- | --- |
| `PartnerCode` | MoneyGuard | External tenant identifier passed to `register` | App configuration; not a secret. Do not hardcode another bank's code. |
| `BankUserReference` | Bank | Stable, high-entropy bearer reference returned after bank authentication | Secret storage only. Never put it in a URL, query string, analytics event, crash report, or log. |
| MoneyGuard partner token | MoneyGuard | Short-lived `partner-api` JWT returned after registration | Memory or encrypted storage only. Never decode it for business logic or log it. |
| `partnerClientInstanceId` | Partner SDK | Identifies a PartnerOnly client installation | The SDK creates and stores it; the bank app must not replace it. |
| `hostInstallationId` | Standalone MoneyGuard app | Identifies the trusted host installation | Never supply or overwrite it from the bank app. |
| `sessionHandle` | MoneyGuard | Correlates the current session across the coordinated system | Treat as opaque. The SDK stores it internally. |
| Bank account reference | Bank | Random positive 63-bit reference to an account | Treat as opaque. Never substitute a database row ID or derive/enumerate values. |

The bank remains the source of truth for customer PII and account data. The general MoneyGuard session and JWT contain references, not a customer profile. `SessionResponse` no longer contains first name, last name, email, account number, or other profile data. Keep bank-owned display data on the bank side.

After registration, authenticated SDK operations use the **MoneyGuard token**. In particular, `policy().getUserAccounts(...)`, `policy().createPolicy(...)`, and `policy().isCustomer(...)` now take the MoneyGuard token; examples that pass a bank user ID and PartnerCode to those methods are obsolete.

## 4. Android requirements

The current AARs require:

- Android `minSdk` 29 or later;
- `compileSdk` 35;
- Java 11 source/target compatibility;
- Kotlin JVM target 11;
- AndroidX.

Example module configuration:

```kotlin
android {
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}
```

## 5. Install the SDK

Copy the complete release set into the app module's `libs/` directory:

```text
app/libs/moneyguard-sdk-release.aar
app/libs/moneyguard-sdk-auth-release.aar
app/libs/moneyguard-sdk-commons-release.aar
```

Do not mix AARs from different builds. Verify their checksums against the delivery manifest:

```bash
shasum -a 256 app/libs/moneyguard-sdk-*.aar
```

Local AARs do not carry Maven dependency metadata, so declare their runtime dependencies explicitly. These versions match the current SDK source release; use the version sheet supplied with a later delivery when it differs.

```kotlin
dependencies {
    implementation(files("libs/moneyguard-sdk-release.aar"))
    implementation(files("libs/moneyguard-sdk-auth-release.aar"))
    implementation(files("libs/moneyguard-sdk-commons-release.aar"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.security:security-crypto:1.0.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.13.0")
    implementation("net.danlew:android.joda:2.13.1")
}
```

Use `google()` and `mavenCentral()` in dependency resolution. Do not repackage the three AARs into one fat AAR unless MoneyGuard has certified that exact output.

### R8 and release builds

The AARs contain consumer rules for Gson models, Retrofit interfaces, coroutines, and IPC types. Keep those embedded rules intact. A debug build does not exercise the full R8 path, so every SDK upgrade must also pass a minified release build and an on-device smoke test.

If release-only parsing or initialization fails, first confirm that all three AARs are from the same delivery. Then inspect the merged consumer rules and mapping rather than adding broad `-keep class ** { *; }` rules.

## 6. Manifest and permissions

Every integration needs Internet access:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

For location-aware login screening, also declare and request location at runtime:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Location denial is a valid outcome. If the partner's geolocation rule is enabled, missing, stale, inaccurate, replayed, invalid, or mocked location causes bank OTP step-up; the app must not fabricate a location or silently disable the rule.

Only add overlay permission when the optional Typing Profile feature is in scope:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

The main SDK AAR already contributes the package visibility query for `com.wimika.moneyguard`, the HTTPS-view intent query, and `TypingDNAOverlayService`. Confirm them in the merged manifest; do not duplicate the service declaration unnecessarily.

Disable clear-text traffic and Android backups in release according to the bank's security standard. Never ship the Sabi simulator's local HTTP URL in a partner release.

## 7. Initialize once

Initialize the SDK with the application context in `Application.onCreate()` and retain one app-wide facade.

```kotlin
class BankApplication : Application() {
    lateinit var moneyGuard: MoneyGuardSdkService
        private set

    override fun onCreate() {
        super.onCreate()
        moneyGuard = MoneyGuardSdk.initialize(this)
    }
}
```

Register the class in the manifest:

```xml
<application
    android:name=".BankApplication"
    ... />
```

`MoneyGuardSdk.initialize(...)` is idempotent. Do not initialize per Activity, create your own internal service locator, or bind to the standalone app directly.

The examples below use coroutines. Registration, credential checks, transaction
checks, claims, location checks, and selected risk operations also expose callback
overloads. Whichever style the app uses, preserve the same state handling and cancel
or detach UI work with the owning lifecycle.

## 8. Register after bank login

### 8.1 Bank login result

The bank first authenticates the customer using its normal login flow. On success, the bank API returns a stable, opaque `BankUserReference`. It must be high entropy, revocable through bank-side session/account controls, and resolvable only by the bank.

For the Sabi simulator the value is named `sessionId`, but it is not a database ID and not a MoneyGuard JWT.

### 8.2 Send a fresh login location

Prefer the location overload for every login. Construct `LoginLocation` from a reading collected for this login attempt:

```kotlin
val loginLocation = LoginLocation(
    latitude = location.latitude,
    longitude = location.longitude,
    accuracyMeters = location.accuracy.toDouble(),
    observedAtUtc = Instant.ofEpochMilli(location.time).toString(),
    isMocked = LocationCompat.isMock(location)
)
```

Do not reuse a cached location merely to avoid step-up. The overload without location remains source compatible, but an enabled bank will receive `BankStepUpRequired` when the observation is unavailable or unacceptable.

### 8.3 Collect the final registration result

Use positional arguments for `register(...)`; the current public interface retains the legacy parameter name `parteBankId`.

```kotlin
val result = moneyGuard.authentication()
    .register(PARTNER_CODE, bankUserReference, loginLocation)
    .first { it !is MoneyGuardResult.Loading }

when (result) {
    is MoneyGuardResult.Success -> handleSessionResponse(
        response = result.data,
        bankUserReference = bankUserReference,
        loginLocation = loginLocation
    )

    is MoneyGuardResult.Failure -> {
        val securityError = result.error as? MoneyGuardSecurityException
        if (securityError != null) {
            // Deterministic security failure. Do not direct-fallback or retry as another mode.
            showMoneyGuardUnavailable(securityError.code)
        } else {
            // Infrastructure failure. Record an explicit degraded state.
            showTemporaryProtectionWarning()
        }
    }

    MoneyGuardResult.Loading -> Unit // excluded by first(...)
}
```

Do not log `bankUserReference`, the MoneyGuard token, credentials, full request/response JSON, or token prefixes. Safe telemetry includes booleans, lengths, result codes, `hostSyncStatus`, latency, and a correlation ID generated by the bank app.

### 8.4 Handle every `SessionResponse`

`MoneyGuardResult.Success` means the SDK completed a meaningful protocol outcome; it does not always mean a usable token was issued.

| `response.result` | Meaning | Required app action |
| --- | --- | --- |
| `TokenGenerated` | Registration completed. | Require a non-blank token, store only what the app needs, and continue. |
| `BankStepUpRequired` | Server-side screening requires bank-owned OTP verification. | Do not call protected SDK APIs. Read the opaque challenge from `screeningDecision`, verify OTP with the bank, then call `completeBankStepUp`. |
| `UntrustedInstallationRequires2Fa` | The host installation is not trusted for this partner. | Do not use the empty/restricted token and do not treat the result as safe. Current B2B2C device replacement has no bearer-token repair route; fail closed for MoneyGuard protection and follow the certified support/recovery journey. |
| `InvalidRequest` or `PartnerUserAuthenticationFailed` | Registration did not authenticate the bank subject. | Do not continue with MoneyGuard APIs. Expire the attempted MoneyGuard state and investigate the bank/backend contract. |

On a token-bearing response, also inspect `hostSyncStatus`:

| `hostSyncStatus` | Meaning | UX/telemetry |
| --- | --- | --- |
| `HOST_ATTACHED` | The installed standalone host accepted authenticated IPC and owns the host telemetry session. | Full host-assisted protection is available. |
| `PARTNER_ONLY` | The standalone app is not installed, so registration completed directly. | The bank session may continue; show install/onboarding guidance where appropriate. |
| `HOST_UNAVAILABLE` | The app is installed but binding/transport failed, and direct registration succeeded. | Continue only under the bank's explicit degraded policy and show/record temporary reduced protection. Retry host attachment on the next authenticated registration. |
| `UNKNOWN` | Older or unclassified response. | Treat protection state as unknown, not HostAttached. |

The SDK persists its partner token, PartnerCode, session handle, host-sync status, and client-instance ID in encrypted preferences. The app still needs the returned token to call token-taking APIs; prefer in-memory session state, or bank-approved encrypted storage if process restoration is required.

`hasActivePolicy` is a login-time hint. Use it to avoid unnecessary host checks, but refresh policy state before a purchase or other decision where current state matters. `highRiskThreshold` is retained for compatibility; transaction decisions now come from server thresholds and `RiskResult.verdict`.

## 9. Complete bank OTP step-up

Login geolocation and transaction screening can require OTP. The OTP belongs to the bank and must never be sent to MoneyGuard.

The secure flow is:

1. Read `screeningDecision.challengeReference` and check `requiredAction == "BankOtpStepUp"`.
2. Ask the customer for an OTP using the bank's existing authentication UX.
3. Send the OTP, challenge reference, bank user reference, and purpose to the bank backend over TLS.
4. The bank verifies the OTP and returns a short-lived, single-use opaque proof.
5. For a login challenge, call `completeBankStepUp(...)` with the original bank reference, challenge, proof, and fresh login location.
6. Accept the session only when the completion response contains a usable MoneyGuard token.

```kotlin
val challenge = response.screeningDecision
    ?.takeIf { it.requiresBankOtp() }
    ?.challengeReference
    ?: return showRegistrationFailure()

val bankProof = bankApi.verifyLoginOtp(
    bankUserReference = bankUserReference,
    challengeReference = challenge,
    otp = otp
)

val completed = moneyGuard.authentication()
    .completeBankStepUp(
        PARTNER_CODE,
        bankUserReference,
        challenge,
        bankProof,
        loginLocation
    )
    .first { it !is MoneyGuardResult.Loading }

val completedSession = (completed as? MoneyGuardResult.Success)?.data
if (completedSession?.token.isNullOrBlank()) {
    showRegistrationFailure()
} else {
    continueWithMoneyGuard(completedSession!!)
}
```

The Sabi test OTP `123456` is simulator-only. Never implement a fixed OTP, generate a proof on-device, reuse a proof, or expose the MoneyGuard-to-bank server API key in the Android app.

### Legacy `trustDevice(...)` warning

The public SDK still contains `authentication().trustDevice(...)` for binary compatibility. The former bearer-only backend mutation is retired and returns HTTP 410. It is not a valid recovery path for `UntrustedInstallationRequires2Fa`. A future partner device-key replacement contract must use a versioned, single-use bank grant; do not build new integrations around `trustDevice(...)` unless MoneyGuard supplies and certifies that replacement contract.

## 10. Logout and session boundaries

Call SDK logout for every explicit bank logout, then clear the bank app's own session state:

```kotlin
moneyGuard.authentication().logout()
bankSessionStore.clear()
```

SDK logout tells the standalone Core service to remove only this Android caller's session. Do not clear standalone app storage, broadcast private IPC messages, or assume that another partner/direct session should be logged out.

The `partnerClientInstanceId` is installation-scoped and should survive normal logout. The standalone app's `hostInstallationId` also survives logout and is not owned by the partner app.

## 11. Protection status and standalone app UX

```kotlin
val utility = moneyGuard.utility()
val installed = utility.isMoneyGuardInstalled()

if (!installed) {
    utility.launchAppInstallation(
        bankCode = PARTNER_CODE,
        bankAppId = applicationContext.packageName
    )
}
```

Open the installed app with:

```kotlin
val launched = utility.launchMoneyGuardApp()
```

Given a current MoneyGuard token, combine policy and installation state with:

```kotlin
val status = utility.checkMoneyguardPolicyStatus(moneyGuardToken)
```

Handle all `MoneyGuardAppStatus` values, including active, no-policy, app-not-installed, cancelled-policy, expired-policy, and inactive variants. Do not equate “app installed” with “customer protected”; active cover and a usable registered session also matter.

`utility().checkLocation(token)` is a legacy post-login risk lookup. The authoritative login geolocation gate now runs server-side during `register(..., loginLocation)` and `completeBankStepUp(...)`. Do not use a post-login location check as a substitute for registration-time location.

## 12. Pre-launch and current risk data

The host-backed pre-launch call returns `StartupRisk`, not a bare list:

```kotlin
val startup = moneyGuard.prelaunch().startup()

if (!startup.moneyGuardActive) {
    showReducedProtectionState()
} else {
    when (startup.preLaunchVerdict.decision) {
        PreLaunchDecision.Launch -> continueNormally()
        PreLaunchDecision.LaunchWithWarning -> showRiskWarning(startup.risks)
        PreLaunchDecision.DoNotLaunch -> applyBankSecurityPolicy(startup.risks)
    }
}
```

For dashboards or remediation flows:

```kotlin
val currentRisks = moneyGuard.riskProfile().getRiskProfile()
val startupRisks = moneyGuard.riskProfile().getStartupRisks()

val playProtectDialogOpened = moneyGuard.riskProfile().showPlayProtectDialog()
moneyGuard.riskProfile().forcePlayProtectScan()
```

These calls require a reachable, compatible standalone host. Catch failures and show “protection unavailable” or “could not check”; an empty list caused by unavailable infrastructure is not evidence of safety.

`RiskScore.value` is earned raw points and `RiskScore.maximum` is available raw points. If presenting a percentage, use the agreed/server-provided percentage instead of independently inventing a normalization.

## 13. Credential compromise checks

Run credential screening only after a usable registration token exists and only when enabled in the agreed bank journey.

```kotlin
val credential = Credential(
    username = username,
    passwordStartingCharactersHash = hashedFragment,
    hashAlgorithm = HashAlgorithm.SHA256,
    domain = "your-bank.example"
)

val final = moneyGuard.authentication()
    .credentialCheck(moneyGuardToken, credential)
    .first { it !is MoneyGuardResult.Loading }

when (final) {
    is MoneyGuardResult.Success -> handleCredentialRisk(final.data.status)
    is MoneyGuardResult.Failure -> recordUnavailableCheck()
    MoneyGuardResult.Loading -> Unit
}
```

Never send a raw password. Hash only the agreed fragment with the declared algorithm, do not log it, and do not retain it after the immediate check. Treat `RISK_STATUS_UNSAFE_CREDENTIALS` as a standalone high-danger signal in transaction UX even when a backend score is otherwise permissive.

## 14. Screen every debit before committing it

Construct the request with the source and destination details known to the bank. Supply `destinationBankCode` when available; it supports bank-specific screening such as destination blacklist checks.

```kotlin
val transaction = DebitTransaction(
    sourceAccountNumber = sourceAccountNumber,
    amount = amount,
    memo = memo,
    destinationBank = destinationBankName,
    destinationAccountNumber = destinationAccountNumber,
    destinationBankCode = destinationBankCode
)

val result = moneyGuard.transactionCheck()
    .checkDebitTransaction(moneyGuardToken, transaction)
```

Use the server's `verdict` before the SDK's advisory status rollup:

| Result | Required reference behavior |
| --- | --- |
| `verdict == BLOCK` | Stop the transfer. Do not show a proceed action. |
| `screeningDecision.requiredAction == BankOtpStepUp` | Recommend cancellation. If the customer continues, verify bank OTP against the supplied challenge before the bank commits the transfer. |
| `verdict == WARN` | Show “Cancel Transfer” as the recommended action and “Proceed Anyway” as an explicit override. |
| `verdict == ALLOW` | Continue unless a standalone high-danger signal below requires escalation. |
| `verdict == null` | Compatibility/degraded case. Use `status` and `risks` as advisory data; never manufacture `ALLOW`. |
| call failure or `success == false` | Record an unavailable check and apply the bank's documented degraded-transaction policy. Do not label the device safe. |

`riskScorePercent` is a **safety percentage**: higher is safer. `riskLevel` is the server band (`Low`, `Medium`, `High`, or `Unknown`) calculated using the partner's current portal thresholds. Do not hardcode threshold values in the app.

The SDK can merge server transaction risks with local pre-launch findings. The reference app therefore escalates the following standalone signals even on `ALLOW`:

- Wi-Fi encryption failure;
- DNS spoofing;
- man-in-the-middle detection;
- identity compromise / `RISK_STATUS_UNSAFE_CREDENTIALS`.

The reference UX requires bank OTP before an override when `riskLevel == "High"` or a standalone high-danger signal is present. A plain medium-band warning may proceed after explicit confirmation. Adapt wording to the bank's approved policy, but do not weaken `BLOCK` or silently turn a warning into success.

See [`CheckDebitScreen.kt`](app/src/main/java/ng/wimika/samplebankapp/ui/screens/CheckDebitScreen.kt) for the complete precedence and dialog flow.

## 15. Policy enrolment

All authenticated policy calls use the MoneyGuard token. Account references returned by the service are opaque.

```kotlin
val policy = moneyGuard.policy()

val limits = policy.getCoverageLimits().getOrThrow().coverageLimits
val options = policy.getPolicyOptions(selectedLimitId).getOrThrow().policyOptions
val accounts = policy.getUserAccounts(moneyGuardToken).getOrThrow().bankAccounts

val created = policy.createPolicy(
    moneyGuardToken = moneyGuardToken,
    policyOptionId = selectedOption.id.toString(),
    coveredAccountIds = selectedAccounts.map { it.id.toString() },
    debitAccountId = selectedDebitAccount.id.toString(),
    autoRenew = autoRenew
).getOrThrow()

val isCustomer = policy.isCustomer(moneyGuardToken).getOrThrow()
```

Integration rules:

- display only the catalogue returned for the current tenant;
- use the exact option and account references returned by the SDK;
- never submit account numbers in place of account references;
- do not infer ownership from a successful catalogue read;
- treat purchase rejection as authoritative—the purchase path fails closed if the option is inactive or belongs to another partner;
- do not calculate premiums, fees, or eligibility independently in the app;
- refresh policy state after successful enrolment.

The catalogue read may degrade to a broader active catalogue when tenant configuration is incomplete so the UI is not empty. Purchase validation deliberately does **not** degrade; it enforces current tenant ownership and active state.

## 16. Claims

Claims use the MoneyGuard token and opaque covered-account reference.

```kotlin
val claim = Claim(
    accountId = coveredAccount.id,
    lossDate = lossDate,
    nameOfIncident = incidentName,
    lossAmount = lossAmount,
    statement = customerStatement
)

val response = moneyGuard.claim().submitClaim(
    sessionToken = moneyGuardToken,
    claim = claim,
    attachments = attachments
)
```

Other operations:

```kotlin
val incidentNames = moneyGuard.claim().getIncidentNames(moneyGuardToken)

val claims = moneyGuard.claim().getClaims(
    sessionToken = moneyGuardToken,
    from = fromDate,
    to = toDate,
    bank = bankFilter,
    claimStatus = ClaimStatus.Submitted
)

val claimDetails = moneyGuard.claim().getClaim(moneyGuardToken, claimId)
```

Create attachments as `MultipartBody.Part` without loading unbounded files into memory. Enforce the bank's approved type/size limits, use the multipart field name `attachments`, and never attach debug logs, tokens, or unrelated customer data.

## 17. Optional content and behavioural services

### Onboarding content

```kotlin
val onboarding = moneyGuard.onboardingInfo()
    .getOnboardingInfo(moneyGuardToken)
    .getOrThrow()
```

Use local, reviewed fallback copy if content retrieval fails.

### Partner-managed in-app content

```kotlin
val content = moneyGuard.inAppContent()
    .getInAppContent(moneyGuardToken, PARTNER_CODE)
    .getOrThrow()
```

The response can contain onboarding slides and dialog copy for unusual location, trusted device, and compromised credentials. Treat it as presentation content, not authorization policy.

### Typing Profile

Typing Profile is optional and requires overlay permission plus product/privacy approval. The capture service targets Android View IDs; Compose screens need a stable hosted `EditText` or another certified integration.

```kotlin
if (Settings.canDrawOverlays(activity)) {
    val typing = moneyGuard.getTypingProfile()
    typing.startService(activity, intArrayOf(targetInputViewId))

    val enrollment = typing.isEnrolled("auth", moneyGuardToken)
    val result = if (enrollment.isEnrolled) {
        typing.verifyTypingProfileForAuth(username, moneyGuardToken)
    } else {
        typing.saveTypingProfileForAuth(username, moneyGuardToken)
    }

    typing.resetService()
    typing.stopService()
}
```

Pause/resume with the screen lifecycle, reset after each attempt, and stop when leaving the capture journey. Typing verification does not replace server login screening and is not a supported way to repair an untrusted host installation.

## 18. Partner backend contract

MoneyGuard must validate the bank reference and obtain bank-owned data without moving PII into the general session path. The Sabi-compatible adapter demonstrates the expected split.

### Android-facing bank APIs

The bank app calls its own backend for:

- normal customer login, which returns the stable `BankUserReference`;
- OTP verification for a MoneyGuard challenge, which returns an opaque, short-lived, single-use proof.

The Android app must never receive the MoneyGuard-to-bank server API key.

### MoneyGuard-to-bank APIs

The Sabi-compatible contract uses TLS plus a bank-issued server API key and the reference in headers:

```text
X-<Bank>-Api-Key: <server-only secret>
x-wimika-partner-token: <BankUserReference>
```

Representative operations are:

| Operation | Purpose | Minimum response data |
| --- | --- | --- |
| `GET /api/v1/session` | Validate the reference before MoneyGuard session issuance | Same opaque bank reference and active/inactive state only |
| `GET /api/v1/customers/me` | Purpose-specific customer contact/display lookup | Approved live fields only |
| `GET /api/v1/customers/underwriting-profile` | Policy/claim underwriting | Only fields approved for that purpose |
| `GET /api/v1/customers/accounts` | List accounts | Opaque account reference, masked/display label, type/currency, active state |
| `GET /api/v1/customers/accounts/{accountReference}` | Resolve one account | Same purpose-limited account shape |
| `POST /api/v1/step-up/proofs/verify` | MoneyGuard verifies and consumes a bank proof | Boolean verified result |

The exact adapter and headers are agreed during onboarding. In every variant:

- keep `BankUserReference` out of URL paths and query strings;
- scope every account lookup by both partner subject and account reference;
- make the session verification response pseudonymous;
- separate display/contact lookup from full underwriting data;
- use a random positive 63-bit external account reference rather than an internal database key;
- authenticate MoneyGuard server-to-server and rotate secrets through the deployment secret store;
- make step-up proof short-lived, purpose-bound, customer-bound, challenge-bound, and single-use;
- never log the bearer reference, OTP, proof, API key, raw profile JSON, or account number.

MoneyGuard must implement and certify a bank adapter before the Android integration can pass end-to-end testing.

## 19. Failure policy

“Fail open” applies to availability of the bank's primary journey; it does not convert an unknown or failed MoneyGuard result into a safe verdict.

| Condition | Classification | Reference behavior |
| --- | --- | --- |
| Standalone app absent | Expected PartnerOnly mode | Continue a valid token-bearing session and offer installation guidance. |
| Host binding/timeout failure followed by valid direct registration | Infrastructure degradation (`HOST_UNAVAILABLE`) | Continue only under documented bank policy; show and record reduced protection. |
| SDK/network call failure | Unknown/unavailable check | Record it and use the bank's explicit degraded rule; never display “safe”. |
| `MoneyGuardSecurityException` such as unauthorized caller/invalid registration | Security rejection | No direct fallback. Do not retry with a different mode or PartnerCode. |
| `BankStepUpRequired` | Expected security challenge | Complete bank OTP proof flow before accepting the MoneyGuard session/action. |
| `UntrustedInstallationRequires2Fa` | Untrusted host | Do not use the restricted token or legacy trust endpoint. Follow certified recovery/support. |
| Transaction `BLOCK` | Authoritative server decision | Stop the transaction. |

Make the degraded state visible in UX and telemetry. Clear it only after a later authenticated registration succeeds in the expected state.

## 20. Test and certification checklist

Before release, test at least:

- first login with no standalone app: valid `PARTNER_ONLY` token;
- login with a supported Play-installed standalone app: `HOST_ATTACHED`;
- installed host transport failure: explicit `HOST_UNAVAILABLE`, not silent full protection;
- unauthorized package or certificate: deterministic security rejection and no direct fallback;
- missing/denied/stale/inaccurate/mocked location when geolocation is enabled: `BankStepUpRequired`;
- valid bank OTP proof: completion issues a token; wrong/replayed/expired proof fails;
- untrusted installation: no protected SDK calls and no bearer-token trust repair;
- active policy versus no policy, cancelled policy, expired policy, and app missing;
- tenant-correct plan/account list and cross-tenant/inactive purchase rejection;
- transaction `ALLOW`, `WARN`, `BLOCK`, destination-account OTP challenge, standalone high-risk escalation, and unavailable check;
- claims with valid/invalid account references and attachment limits;
- logout removes only this caller's MoneyGuard session;
- minified release build initializes, registers, parses every used DTO, and completes IPC;
- upgrade from the previously supported SDK without stale AAR/cache behavior.

For the sample app, explicitly set the simulator base URL on **every** build. The deployed Sabi simulator is:

```bash
SABI_BANK_BASE_URL=https://moneyguard-sabi-bank.azurewebsites.net/ \
  ./gradlew :app:assembleDebug

SABI_BANK_BASE_URL=https://moneyguard-sabi-bank.azurewebsites.net/ \
  ./gradlew :app:testDebugUnitTest

SABI_BANK_BASE_URL=https://moneyguard-sabi-bank.azurewebsites.net/ \
  ./gradlew :app:assembleRelease
```

The sample release is debug-signed for controlled testing and is not a production artifact. A real bank must use its controlled release signing and register the resulting certificate identity with MoneyGuard.

If a rebuilt AAR behaves like an older one, stop Gradle daemons, confirm checksums and consumer paths, remove the consuming app's project `.gradle` cache and the relevant Gradle transform cache, then rebuild. Do not infer consumption merely from a similarly named AAR elsewhere in the repository.

Useful device logging filters are:

```bash
adb logcat | grep -E 'MONEYGUARD_LOGGER|MG_SDK_TRACE|MONEYGUARD_RISK_AUDIT'
```

Keep test logs free of raw tokens, bank references, OTPs, proofs, keys, full credential JSON, and customer PII.

## 21. Common migration mistakes

| Stale integration | Current contract |
| --- | --- |
| Read names/profile from `SessionResponse` | Session response is pseudonymous; use bank-owned UI data. |
| Pass bank user ID + PartnerCode into policy methods | Pass the MoneyGuard token to `getUserAccounts`, `createPolicy`, and `isCustomer`. |
| Treat no standalone app and broken installed host as the same | Distinguish `PARTNER_ONLY` from `HOST_UNAVAILABLE`. |
| Direct-fallback after caller authorization/attestation rejection | Security rejection is hard; only transport/runtime unavailability can degrade. |
| Call policy/risk APIs after `UntrustedInstallationRequires2Fa` | Route to certified recovery; its token is not usable. |
| Repair device trust with `trustDevice(token, ...)` | Legacy bearer trust endpoint is retired (HTTP 410). |
| Run location only after login | Supply a fresh `LoginLocation` during registration; server screening owns the decision. |
| Hardcode risk thresholds or score locally | Use `riskScorePercent`, `riskLevel`, and `verdict` returned by the server. |
| Treat a failed transaction check as `ALLOW` | Mark it unavailable and apply a documented bank degraded rule. |
| Use account numbers/database IDs in policy or claims | Use only returned opaque account references. |
| Clear all MoneyGuard host state on bank logout | Call SDK logout; Core removes only the caller-scoped session. |
| Validate only a debug build | Build and smoke-test the minified release variant. |

## 22. Troubleshooting

### `caller_not_authorised`

Confirm the runtime application ID and SHA-256 of the actual installed APK/Play signing certificate, the enabled Android-client registry entry, and the matching PartnerCode. This is a security failure; do not force PartnerOnly fallback.

### Host remains PartnerOnly or reports `HOST_UNAVAILABLE`

Confirm `com.wimika.moneyguard` is installed from the supported distribution track, the host version matches the AAR delivery, Play recognition succeeds, package visibility is merged, and Binder connection is not being blocked. Use the returned state and IPC error code rather than guessing from installation alone.

### Registration returns HTTP 500 or every partner lookup fails

Escalate to MoneyGuard with correlation ID and time. A green `/api/version` is not database proof; the staging deployment must also have the expected schema and a successful authenticated session registration for the partner.

### Plans or accounts are wrong

Confirm the MoneyGuard JWT belongs to the expected partner, the partner directory/base URL is correct, account references are scoped to that bank subject, and plan rows are active for the partner. Do not work around a purchase rejection by selecting an option from a broader catalogue.

### Debug works but release fails

Confirm the three AAR hashes, run a clean minified build, inspect merged consumer rules/mapping, and test DTO parsing and Retrofit initialization. This is commonly a mixed/stale AAR or R8-consumption issue.

### Transaction result is unexpected

Capture `verdict`, `riskLevel`, `riskScorePercent`, `autoBlockEnabled`, risk names/statuses, screening reason/action, and correlation ID—without transaction account data. Verify the bank's current portal thresholds and risk weights; the app must not apply an old local threshold.

## 23. Support and license

For onboarding, artifact checksums, environment access, certification, and technical support, contact [tech@wimika.ng](mailto:tech@wimika.ng).

The MoneyGuard Partner SDK is proprietary software owned by Wimika RMS. All rights reserved.
