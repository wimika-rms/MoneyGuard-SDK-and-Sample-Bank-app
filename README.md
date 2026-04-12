# MoneyGuard Partner SDK for Android

The MoneyGuard Partner SDK is an Android library that enables partner banks to embed MoneyGuard protection directly into their Android applications. It acts as a secure gateway for device pre-launch risk checks, continuous authentication signals, credential compromise checks, transaction risk evaluation, policy enrollment support, claims APIs, behavioral biometrics, and partner-configurable in-app content, while offloading heavier operational workflows to the standalone MoneyGuard application.

This SDK is designed for banking environments where security, performance, and low-friction user experience are equally important. Its core principle is **fail-open, non-blocking integration**: MoneyGuard should improve trust decisions without becoming a point of failure for your primary banking journey.

---

## 1. What the SDK exposes

`MoneyGuardSdkService` exposes the following services:

- `authentication()`
- `utility()`
- `prelaunch()`
- `transactionCheck()`
- `policy()`
- `claim()`
- `getTypingProfile()`
- `onboardingInfo()`
- `inAppContent()`
- `riskProfile()`


---

## 2. Integration concepts you must understand first

### 2.1 The three important identifiers

Your integration will typically deal with three different values:

1. **Partner bank ID**  
   This identifies your institution inside MoneyGuard. A partner ID will be generated for you once you get onboarded in the MoneyGuard Partner Portal.

2. **Bank session ID / partner session token**  
   This is your own authenticated user/session reference from your banking platform. It is a unique identifier that MoneyGuard can use to retrieve your user's information. It is passed into MoneyGuard during registration and is also used by some policy-related SDK APIs.

3. **MoneyGuard token**  
   This is returned by `authentication().register(...)` and is required for most authenticated MoneyGuard API calls.

### 2.2 Which token goes where

Use the **MoneyGuard token** for:
- credential checks
- location checks
- claim APIs
- onboarding info
- in-app content
- transaction checks
- typing-profile API calls
- protection status checks

Use the **bank user/session identifier plus partner ID** for:
- `policy().getUserAccounts(...)`
- `policy().createPolicy(...)`
- `policy().isCustomer(...)`

### 2.3 Fail-open expectation

The SDK is intentionally designed so that risk checks do not block your banking app by default when the failure is infrastructural, for example:
- temporary network failure
- timeout
- MoneyGuard standalone app unavailable
- non-critical SDK exception

Your app should log errors, preserve user safety messaging where appropriate, and continue the primary flow unless your own policy says the risk outcome should block the journey.

---

## 3. Requirements

The current Partner Lite SDK requires:

- **Android minSdk 29 or higher**
- **compileSdk 35**
- **Java 11 / JVM target 11**
- **AndroidX**

If your app currently targets lower Android versions, upgrade planning is required before adopting this SDK.

---

## 4. Installation

Add the SDK AARs to your app and include them in your module dependencies:

```gradle
dependencies {
    implementation(files("libs/moneyguard-sdk-release.aar"))
    implementation(files("libs/moneyguard-sdk-commons-release.aar"))
    implementation(files("libs/moneyguard-sdk-auth-release.aar"))

    // Required external dependencies used by the SDK
    implementation("com.squareup.okhttp3:okhttp:<version>")
    implementation("com.squareup.okhttp3:logging-interceptor:<version>")
    implementation("com.squareup.retrofit2:retrofit:<version>")
    implementation("com.squareup.retrofit2:converter-gson:<version>")
    implementation("com.google.code.gson:gson:<version>")
    implementation("joda-time:joda-time:<version>")
}
````

> Because these are local AARs, keep your dependency versions aligned with the SDK release package supplied by Wimika.

---

## 5. Manifest configuration

Add Internet permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

If you plan to use **Typing Profile / behavioural biometrics**, also add:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

> `SYSTEM_ALERT_WINDOW` is only needed for typing-profile overlay capture. Do not request it unless you are integrating behavioural biometrics.

The SDK library already declares the MoneyGuard package query and the typing overlay service internally, so you normally do not need to copy those declarations manually.

---

## 6. SDK initialization

Initialize the SDK once in your custom `Application` class:

```kotlin
class YourBankApplication : Application() {

    companion object {
        var sdkService: MoneyGuardSdkService? = null
    }

    override fun onCreate() {
        super.onCreate()
        sdkService = MoneyGuardSdk.initialize(this)
    }
}
```

Recommended:

* keep a single app-wide SDK reference
* initialize once at app startup
* store returned tokens and identifiers securely

---

## 7. Recommended end-to-end integration flow

A practical partner flow usually looks like this:

1. Your user logs into your banking app.
2. Your backend returns your own bank session identifier.
3. Call `authentication().register(...)` using:

   * your partner bank ID
   * logged in user session token / ID
4. Persist the returned:

   * MoneyGuard token
   * installation ID
   * user first/last name
   * high-risk threshold if you use it
5. Run post-login checks:

   * credential compromise check
   * unusual-location check
   * typing verification if required
   * trust-device flow if verification succeeds
6. Use policy, claims, transaction, and content APIs in your journeys.
7. On logout, clear local MoneyGuard state and call `authentication().logout()`.

---

## 8. Authentication and session management

## 8.1 Registering a MoneyGuard session

After your own bank login succeeds, register the user with MoneyGuard.

### Suspend / Flow version

```kotlin
lifecycleScope.launch {
    YourBankApplication.sdkService
        ?.authentication()
        ?.register(Constants.PARTNER_BANK_ID, bankSessionId)
        ?.collect { result ->
            when (result) {
                is MoneyGuardResult.Loading -> {
                    // optional loading state
                }

                is MoneyGuardResult.Success -> {
                    val response = result.data

                    val moneyGuardToken = response.token
                    val installationId = response.installationId
                    val hasActivePolicy = response.hasActivePolicy
                    val sessionResultFlag = response.result

                    // Save securely
                    preferenceManager.saveMoneyGuardToken(moneyGuardToken)
                    preferenceManager.saveMoneyGuardInstallationId(installationId)
                    preferenceManager.saveUserFirstName(response.userDetails.firstName)
                    preferenceManager.saveUserLastName(response.userDetails.lastName)
                    preferenceManager.saveHighRiskThreshold(response.highRiskThreshold)
                }

                is MoneyGuardResult.Failure -> {
                    // fail-open: log and continue safely
                }
            }
        }
}
```

### Callback version

```kotlin
YourBankApplication.sdkService
    ?.authentication()
    ?.register(Constants.PARTNER_BANK_ID, bankSessionId) { result ->
        when (result) {
            is MoneyGuardResult.Loading -> { }
            is MoneyGuardResult.Success -> { /* save response data */ }
            is MoneyGuardResult.Failure -> { /* fail-open handling */ }
        }
    }
```

### Important notes

* Prefer **positional arguments** in `register(...)` because the public API currently uses the parameter name `parteBankId`, which makes named-argument examples brittle.
* Persist the MoneyGuard token securely, because most subsequent SDK calls require it.
* Persist the installation ID too; it is used later in trust-device flows.

## 8.2 Handling session result flags

The `SessionResponse.result` field may tell you that the installation is untrusted and further verification is required.

A recommended pattern is:

* if registration succeeds and the result indicates an untrusted installation, show a trusted-device / verification dialog
* route the user into your typing verification or step-up flow
* after successful verification, call `trustDevice(...)`

## 8.3 Logging out

```kotlin
YourBankApplication.sdkService?.authentication()?.logout()
```

Also clear any locally stored:

* MoneyGuard token
* installation ID
* cached user names
* partner/bank session references you no longer need

---

## 9. Trusting a device

The SDK supports explicit device trust after a successful verification journey.

### Suspend / Flow version

```kotlin
lifecycleScope.launch {
    val deviceId = preferenceManager.getMoneyGuardInstallationId() ?: return@launch
    val token = preferenceManager.getMoneyGuardToken() ?: return@launch
    val bankSessionId = preferenceManager.getBankSessionId() ?: return@launch

    val request = TrustedDeviceRequest(
        userId = bankSessionId,
        installationId = deviceId,
        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
    )

    YourBankApplication.sdkService
        ?.authentication()
        ?.trustDevice(token, deviceId, request)
        ?.collect { result ->
            when (result) {
                is MoneyGuardResult.Loading -> { }
                is MoneyGuardResult.Success -> {
                    // Device trusted successfully
                }
                is MoneyGuardResult.Failure -> {
                    // Show fallback message or allow controlled continuation
                }
            }
        }
}
```

### Callback version

```kotlin
YourBankApplication.sdkService
    ?.authentication()
    ?.trustDevice(token, deviceId, request) { result ->
        when (result) {
            is MoneyGuardResult.Loading -> { }
            is MoneyGuardResult.Success -> { }
            is MoneyGuardResult.Failure -> { }
        }
    }
```

Use this after your chosen identity-verification or behavioural-verification step succeeds.

---

## 10. Utility service

The Utility service helps you:

* detect whether the standalone MoneyGuard app is installed
* launch the standalone app
* open the install route for the standalone app
* inspect protection status
* run unusual-location checks

## 10.1 Check whether MoneyGuard is installed

```kotlin
val isInstalled = YourBankApplication.sdkService
    ?.utility()
    ?.isMoneyGuardInstalled() == true
```

## 10.2 Launch installation flow

```kotlin
YourBankApplication.sdkService
    ?.utility()
    ?.launchAppInstallation(
        bankCode = Constants.PARTNER_BANK_ID,
        bankAppId = context.packageName
    )
```

## 10.3 Launch the standalone MoneyGuard app

```kotlin
val launched = YourBankApplication.sdkService
    ?.utility()
    ?.launchMoneyGuardApp() == true

if (!launched) {
    // fallback: show install / retry / help UI
}
```

## 10.4 Check protection status

```kotlin
lifecycleScope.launch {
    val token = preferenceManager.getMoneyGuardToken() ?: return@launch

    val status = YourBankApplication.sdkService
        ?.utility()
        ?.checkMoneyguardStatus(token)

    when (status) {
        MoneyGuardAppStatus.Active -> { /* protected */ }
        MoneyGuardAppStatus.ValidPolicyAppNotInstalled -> { /* valid policy, app missing */ }
        MoneyGuardAppStatus.NoPolicyAppInstalled -> { /* app installed, no policy */ }
        MoneyGuardAppStatus.InActive -> { /* inactive */ }
        else -> { /* handle cancelled/expired variants */ }
    }
}
```

You may also use:

```kotlin
val policyStatus = YourBankApplication.sdkService
    ?.utility()
    ?.checkMoneyguardPolicyStatus(token)
```

## 10.5 Unusual-location check

### Suspend version

```kotlin
lifecycleScope.launch {
    val token = preferenceManager.getMoneyGuardToken() ?: return@launch

    try {
        val response = YourBankApplication.sdkService
            ?.utility()
            ?.checkLocation(token)

        if (response?.data?.isNotEmpty() == true) {
            // treat as unusual / unsafe location
        } else {
            // safe to proceed
        }
    } catch (e: Exception) {
        // fail-open
    }
}
```

### Callback version

```kotlin
YourBankApplication.sdkService
    ?.utility()
    ?.checkLocation(
        token,
        onSuccess = { response ->
            if (response.data.isNotEmpty()) {
                // unusual location
            } else {
                // safe
            }
        },
        onFailure = {
            // fail-open
        }
    )
```

---

## 11. Pre-launch checks

Use pre-launch checks to inspect device and environment posture before authentication.

The prelaunch service exposes:

* `startup()` suspend version
* `startup(onResult)` callback version
* `riskChecks()` helper string output

## 11.1 Running startup checks

```kotlin
lifecycleScope.launch {
    val startupRisk = YourBankApplication.sdkService
        ?.prelaunch()
        ?.startup() ?: return@launch

    when (startupRisk.preLaunchVerdict.decision) {
        PreLaunchDecision.Launch -> {
            // safe
        }
        PreLaunchDecision.LaunchWithWarning -> {
            // warn but continue
        }
        PreLaunchDecision.DoNotLaunch -> {
            // block or route to support flow, based on your policy
        }
    }
}
```

## 11.2 Simpler pattern: filter only risky items

Many partner apps will find this pattern easier:

```kotlin
lifecycleScope.launch {
    val startupRisk = YourBankApplication.sdkService
        ?.prelaunch()
        ?.startup() ?: return@launch

    val riskyItems = startupRisk.risks.filter {
        it.status == RiskStatus.RISK_STATUS_WARN ||
        it.status == RiskStatus.RISK_STATUS_UNSAFE
    }

    if (riskyItems.isNotEmpty()) {
        // show sequential warnings or a consolidated risk dialog
    }
}
```

## 11.3 Important behavior

If the standalone MoneyGuard app is not installed or cannot be reached, the SDK may return `moneyGuardActive = false` with an empty risk list. Handle this gracefully and do not assume that “no risks” means the full protection stack is active.

---

## 12. Credential compromise checks

Use credential checks after successful registration, typically during login or step-up authentication.

### Suspend / Flow version

```kotlin
val credential = Credential(
    username = username.trim(),
    passwordStartingCharactersHash = hashedPasswordFragment,
    domain = "yourbank.com",
    hashAlgorithm = HashAlgorithm.SHA256
)

lifecycleScope.launch {
    YourBankApplication.sdkService
        ?.authentication()
        ?.credentialCheck(moneyGuardToken, credential)
        ?.collect { result ->
            when (result) {
                is MoneyGuardResult.Loading -> { }
                is MoneyGuardResult.Success -> {
                    when (result.data.status) {
                        RiskStatus.RISK_STATUS_UNSAFE -> {
                            // require password reset or stronger verification
                        }
                        else -> {
                            // continue
                        }
                    }
                }
                is MoneyGuardResult.Failure -> {
                    // fail-open
                }
            }
        }
}
```

### Callback version

```kotlin
YourBankApplication.sdkService
    ?.authentication()
    ?.credentialCheck(moneyGuardToken, credential) { result ->
        when (result) {
            is MoneyGuardResult.Loading -> { }
            is MoneyGuardResult.Success -> { }
            is MoneyGuardResult.Failure -> { }
        }
    }
```

### Important security note

Never send raw passwords to the SDK. Only send the hashed password fragment expected by your integration contract.

---

## 13. Transaction checks

Use transaction checks before authorizing debit or transfer transactions.

## 13.1 Request model

```kotlin
val transaction = DebitTransaction(
    sourceAccountNumber = "1234567890",
    amount = 50000.0,
    memo = "Rent payment",
    destinationBank = "057",
    destinationAccountNumber = "0987654321"
)
```

## 13.2 Suspend version

```kotlin
lifecycleScope.launch {
    try {
        val result = YourBankApplication.sdkService
            ?.transactionCheck()
            ?.checkDebitTransaction(moneyGuardToken, transaction)
            ?: return@launch

        when (result.status) {
            RiskStatus.RISK_STATUS_SAFE -> {
                // proceed normally
            }
            RiskStatus.RISK_STATUS_WARN -> {
                // confirm / warn / step up
            }
            RiskStatus.RISK_STATUS_UNSAFE_LOCATION,
            RiskStatus.RISK_STATUS_UNSAFE_CREDENTIALS,
            RiskStatus.RISK_STATUS_UNSAFE -> {
                // block or enforce stronger verification
            }
            else -> {
                // handle any additional statuses your implementation supports
            }
        }
    } catch (e: Exception) {
        // fail-open per your bank policy
    }
}
```

## 13.3 Callback version

```kotlin
YourBankApplication.sdkService
    ?.transactionCheck()
    ?.checkDebitTransaction(
        moneyGuardToken,
        transaction,
        onSuccess = { result ->
            // inspect result.status and result.risks
        },
        onFailure = {
            // fail-open
        }
    )
```

## 13.4 Important implementation note

The final result can include both:

* backend/API transaction risks
* SDK prelaunch/startup risks

So treat the result as a combined trust decision, not just a pure backend transfer score.

---

## 14. Policy service

Use the Policy service to power account-protection onboarding and enrollment journeys inside your bank app.

## 14.1 Get coverage limits

```kotlin
lifecycleScope.launch {
    val result = YourBankApplication.sdkService
        ?.policy()
        ?.getCoverageLimits()

    result?.fold(
        onSuccess = { response ->
            val limits = response.coverageLimits
        },
        onFailure = { error ->
            // show retry state
        }
    )
}
```

## 14.2 Get policy options for a coverage limit

```kotlin
lifecycleScope.launch {
    val result = YourBankApplication.sdkService
        ?.policy()
        ?.getPolicyOptions(coverageLimitId = 1)

    result?.fold(
        onSuccess = { response ->
            val options = response.policyOptions
        },
        onFailure = { error ->
            // handle error
        }
    )
}
```

## 14.3 Get user accounts

This call uses the **bank user/session identifier**, not the MoneyGuard token.

```kotlin
lifecycleScope.launch {
    val bankSessionId = preferenceManager.getBankSessionId() ?: return@launch

    val result = YourBankApplication.sdkService
        ?.policy()
        ?.getUserAccounts(
            userId = bankSessionId,
            partnerId = Constants.PARTNER_BANK_ID
        )

    result?.fold(
        onSuccess = { response ->
            val accounts = response.bankAccounts
        },
        onFailure = { error ->
            // handle error
        }
    )
}
```

## 14.4 Create policy

This also uses the **bank user/session identifier** plus partner ID.

```kotlin
lifecycleScope.launch {
    val bankSessionId = preferenceManager.getBankSessionId() ?: return@launch

    val result = YourBankApplication.sdkService
        ?.policy()
        ?.createPolicy(
            userId = bankSessionId,
            partnerId = Constants.PARTNER_BANK_ID,
            policyOptionId = selectedPolicyOptionId,
            coveredAccountIds = selectedAccountIds,
            debitAccountId = selectedDebitAccountId,
            autoRenew = true
        )

    result?.fold(
        onSuccess = {
            // policy created
        },
        onFailure = { error ->
            // show enrollment failure
        }
    )
}
```

## 14.5 Check whether a user is already a customer

```kotlin
lifecycleScope.launch {
    val bankSessionId = preferenceManager.getBankSessionId() ?: return@launch

    val result = YourBankApplication.sdkService
        ?.policy()
        ?.isCustomer(
            userId = bankSessionId,
            partnerId = Constants.PARTNER_BANK_ID
        )

    result?.fold(
        onSuccess = { isCustomer ->
            // adapt journey
        },
        onFailure = {
            // handle gracefully
        }
    )
}
```

---

## 15. Claims service

Use the Claims service for lightweight claims capabilities within your app.

## 15.1 Submit a claim

```kotlin
val claim = Claim(
    accountId = selectedAccountId,
    lossDate = Date(),
    nameOfIncident = "Fraudulent Transfer",
    lossAmount = 50000.0,
    statement = "I noticed an unauthorized transfer."
)
```

Convert files into `MultipartBody.Part` attachments using your own URI/file helper, then submit:

### Suspend version

```kotlin
lifecycleScope.launch {
    try {
        val response = YourBankApplication.sdkService
            ?.claim()
            ?.submitClaim(moneyGuardToken, claim, attachments)

        if (response?.success == true) {
            // success
        } else {
            // show message
        }
    } catch (e: Exception) {
        // handle error
    }
}
```

### Callback version

```kotlin
YourBankApplication.sdkService
    ?.claim()
    ?.submitClaim(
        moneyGuardToken,
        claim,
        attachments,
        onSuccess = { response ->
            // success
        },
        onFailure = { error ->
            // failure
        }
    )
```

### Attachment notes

* The sample app sends attachments using multipart field name `attachments`.
* Build `MultipartBody.Part` from content URIs carefully and avoid loading very large files into memory.

## 15.2 Get incident names

```kotlin
lifecycleScope.launch {
    val names = YourBankApplication.sdkService
        ?.claim()
        ?.getIncidentNames(moneyGuardToken)
}
```

Or callback version:

```kotlin
YourBankApplication.sdkService
    ?.claim()
    ?.getIncidentNames(
        moneyGuardToken,
        onSuccess = { names -> },
        onFailure = { error -> }
    )
```

## 15.3 Get claims list

```kotlin
lifecycleScope.launch {
    val claims = YourBankApplication.sdkService
        ?.claim()
        ?.getClaims(
            sessionToken = moneyGuardToken,
            from = fromDate,
            to = toDate,
            bank = "",
            claimStatus = ClaimStatus.Submitted
        )
}
```

## 15.4 Get a single claim

```kotlin
lifecycleScope.launch {
    val claim = YourBankApplication.sdkService
        ?.claim()
        ?.getClaim(moneyGuardToken, claimId = 123)
}
```

---

## 16. Typing Profile / behavioural biometrics

The Typing Profile service supports:

* overlay service control
* typing-pattern extraction
* enrolment
* verification
* auth-specific enrolment and verification
* enrolment-state checks

## 16.1 Runtime permission requirement

Typing Profile requires overlay permission. Request it only for flows where behavioural biometrics is enabled.

Before calling `startService(...)`, check overlay permission:

```kotlin
if (Settings.canDrawOverlays(context)) {
    lifecycleScope.launch {
        YourBankApplication.sdkService
            ?.getTypingProfile()
            ?.startService(activity, intArrayOf(TARGET_INPUT_ID))
    }
} else {
    // route user to overlay permission settings
}
```

## 16.2 Start capture service

```kotlin
lifecycleScope.launch {
    YourBankApplication.sdkService
        ?.getTypingProfile()
        ?.startService(this@LoginActivity, intArrayOf(R.id.edittext_username))
}
```

For Jetpack Compose, wrap a traditional `EditText` inside `AndroidView` and assign a stable ID.

## 16.3 Service lifecycle controls

```kotlin
val typing = YourBankApplication.sdkService?.getTypingProfile()

typing?.pauseService()
typing?.resumeService()
typing?.resetService()
typing?.stopService()
```

Recommended:

* call `resetService()` after each completed enrolment or verification
* call `stopService()` when leaving the typing screen or after the flow finishes

## 16.4 Get typing pattern locally

```kotlin
val pattern = YourBankApplication.sdkService
    ?.getTypingProfile()
    ?.getTypingPattern("hello, my name is John")
```

## 16.5 General enrolment / verification APIs

```kotlin
lifecycleScope.launch {
    val typing = YourBankApplication.sdkService?.getTypingProfile() ?: return@launch

    val saveResult = typing.saveTypingProfile(text = username, token = moneyGuardToken)
    val verifyResult = typing.verifyTypingProfile(text = username, token = moneyGuardToken)
    val matchResult = typing.matchTypingProfile(text = username, token = moneyGuardToken)
}
```

## 16.6 Auth-specific enrolment / verification

These are the methods most partners will use during login hardening:

```kotlin
lifecycleScope.launch {
    val typing = YourBankApplication.sdkService?.getTypingProfile() ?: return@launch

    val enrollment = typing.saveTypingProfileForAuth(username, moneyGuardToken)
    val verification = typing.verifyTypingProfileForAuth(username, moneyGuardToken)
}
```

Typical pattern:

* on first successful login(s), collect auth enrolment data using `saveTypingProfileForAuth(...)`
* on later high-risk or untrusted-device logins, use `verifyTypingProfileForAuth(...)`
* if verification succeeds, optionally call `trustDevice(...)`

## 16.7 Check whether user is enrolled

```kotlin
lifecycleScope.launch {
    val enrolled = YourBankApplication.sdkService
        ?.getTypingProfile()
        ?.isEnrolled(type = "auth", token = moneyGuardToken)
}
```

---

## 17. Onboarding Info service

Use this service to fetch partner onboarding content for account-protection journeys.

```kotlin
lifecycleScope.launch {
    val result = YourBankApplication.sdkService
        ?.onboardingInfo()
        ?.getOnboardingInfo(moneyGuardToken)

    result?.fold(
        onSuccess = { response ->
            val items = response.infoList
            val learnMoreUrl = response.learnMoreUrl
        },
        onFailure = {
            // fallback UI
        }
    )
}
```

This is useful for:

* “why protect my account?” screens
* onboarding slides
* learn-more flows

---

## 18. In-App Content service

Use this to fetch partner-specific dialog and content payloads managed by MoneyGuard.

```kotlin
lifecycleScope.launch {
    val result = YourBankApplication.sdkService
        ?.inAppContent()
        ?.getInAppContent(
            token = moneyGuardToken,
            partnerBankId = Constants.PARTNER_BANK_ID
        )

    result?.fold(
        onSuccess = { response ->
            val onboardingSlides = response.onboardingSlides
            val unusualLocationDialog = response.unusualLocationDialog
            val trustedDeviceDialog = response.trustedDeviceDialog
            val compromisedCredentialDialog = response.compromisedCredentialDialog
        },
        onFailure = {
            // fallback to local copy
        }
    )
}
```

Recommended use cases:

* unusual-location dialog title/body
* untrusted-device dialog copy
* compromised-credential dialog copy
* partner-managed onboarding slides

---

## 19. Risk Profile service

Use this service when you need direct access to the current or startup risk set.

```kotlin
lifecycleScope.launch {
    val startupRisks = YourBankApplication.sdkService
        ?.riskProfile()
        ?.getStartupRisks()

    val currentRiskProfile = YourBankApplication.sdkService
        ?.riskProfile()
        ?.getRiskProfile()
}
```

This service is especially useful for:

* security dashboards
* internal diagnostics
* advanced partner risk visualizations

---

## 20. Recommended secure storage

At minimum, store the following securely:

* MoneyGuard token
* installation ID
* bank session ID if your policy flow depends on it
* user first/last name if used in typing journeys
* any high-risk threshold returned during registration

Use encrypted local storage where possible.

---

## 21. Error-handling guidance

Recommended default posture:

* **Authentication register failure:** log and continue with bank login if your policy allows
* **Credential check failure:** fail-open, but consider telemetry/logging
* **Location check failure:** fail-open unless your bank policy requires step-up
* **Transaction check failure:** fail-open or step-up, depending on business policy
* **Typing-profile failure:** do not break normal login unless your product explicitly requires behavioural verification
* **In-app content failure:** fall back to hardcoded UX copy
* **Standalone app missing:** show install guidance, but avoid dead-end UX

---

## 22. Troubleshooting

### The MoneyGuard app does not launch

* confirm the standalone app is installed
* call `isMoneyGuardInstalled()` first
* if not installed, call `launchAppInstallation(bankCode, bankAppId)`

### Typing profile never starts

* confirm overlay permission is granted
* confirm you passed the correct Activity
* confirm your target input IDs are real view IDs
* call `resetService()` and `stopService()` after each completed flow to avoid stale state

### Policy APIs return wrong data

* verify that you are using the **bank session ID/user ID** and **partner ID**
* do not use the MoneyGuard token for `getUserAccounts`, `createPolicy`, or `isCustomer`

### Claims submission fails

* confirm attachment conversion to `MultipartBody.Part`
* validate MIME types and file accessibility
* check token validity before submitting

### Unexpected transaction status

* remember that the result can combine:

  * backend transaction risks
  * startup/device risks

---

## 23. Recommended reference journeys

### Login hardening

1. bank login
2. MoneyGuard register
3. prelaunch checks
4. credential check
5. unusual-location check
6. typing verification if required
7. trust device if verification succeeds
8. continue to dashboard

### Account protection enrollment

1. get onboarding info
2. get in-app content
3. get user accounts
4. get coverage limits
5. get policy options
6. create policy
7. optionally route to standalone app if needed

### Fraud claims

1. get incident names
2. get covered accounts
3. submit claim
4. show claims list
5. fetch claim details

---

## 24. Support

For technical support, implementation reviews, or release-package alignment, contact:

* **Email:** [tech@wimika.ng](mailto:tech@wimika.ng)

---

## 25. License

This SDK is proprietary software owned by Wimika RMS. All rights reserved.

```

