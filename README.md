# MoneyGuard SDK

The MoneyGuard SDK is a powerful Android library that enables seamless integration of MoneyGuard's financial protection services into your Android application. This SDK provides a comprehensive set of features for managing policies, claims, and transaction security.

## Features

- **Authentication**: Secure user authentication and session management
- **Policy Management**: Create and manage insurance policies
- **Claims Processing**: Submit and track insurance claims
- **Transaction Security**: Real-time transaction monitoring and security checks
- **Utility Functions**: Various utility functions for app integration
- **Pre-launch Checks**: Risk assessment during app startup

## Installation

Add the following dependency to your app's `build.gradle` file:

```gradle
dependencies {
    implementation(files("libs/moneyguard-sdk-release.aar"))
    implementation(files("libs/moneyguard-sdk-commons-release.aar"))
    implementation(files("libs/moneyguard-sdk-auth-release.aar"))


    //Ensure the add the external libraries used by the SDK. 
    implementation(libs.okhttp3)
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.okhttp3.logging)
    implementation(libs.gson.converter)
    implementation(libs.android.joda)
    implementation(libs.coil.compose)
}
```

### Required Manifest Permissions

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

The INTERNET permission is required for the SDK to communicate with MoneyGuard's services.

### Application Configuration

If you're using a custom Application class, make sure to initialize the SDK in your `onCreate()` method:

```kotlin
class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize MoneyGuard SDK
        val sdkService = MoneyGuardSdk.initialize(this)
    }
}
```

Then declare your Application class in the `AndroidManifest.xml`:

```xml
<application
    android:name=".YourApplication"
    ... >
    <!-- Your activities and other configurations -->
</application>
```

## Usage

### Utility Functions

```kotlin
// Check if MoneyGuard app is installed
val isInstalled = sdkService.utility().isMoneyGuardInstalled()

// Launch MoneyGuard app installation
sdkService.utility().launchAppInstallation()

// Launch MoneyGuard app
val launched = sdkService.utility().launchMoneyGuardApp()

// Check MoneyGuard status
val status = sdkService.utility().checkMoneyguardStatus(token)
```



### Pre-launch Checks

The prelaunch checks feature performs risk assessment during app startup to ensure the environment is secure and suitable for financial operations. This helps prevent fraud and ensures compliance with security requirements.

```kotlin
// Perform startup checks
val startupRisk = sdkService.prelaunch().startup()

// Using callback
sdkService.prelaunch().startup { risk ->
    when (risk.preLaunchVerdict.decision) {
        PreLaunchDecision.Launch -> {
            // App can proceed normally
        }
        PreLaunchDecision.LaunchWithWarning -> {
            // Show warning to user but allow proceeding
        }
        PreLaunchDecision.DoNotLaunch -> {
            // App should not proceed
            // Implement appropriate security measures
        }
    }
}
```

#### Risk Assessment Results

The prelaunch checks return a `StartupRisk` object that contains:

- `moneyGuardActive`: Boolean indicating if MoneyGuard is active
- `risks`: List of `SpecificRisk` objects containing detailed risk information
- `preLaunchVerdict`: A `PreLaunchVerdict` object containing:
    - `decision`: The final launch decision (`Launch`, `LaunchWithWarning`, or `DoNotLaunch`)
    - `reasons`: List of reasons for the decision

#### Risk Types and Decisions

The SDK evaluates several types of risks and makes decisions based on their severity:

1. **Critical Risks (DoNotLaunch)**:
    - Root/Jailbreak detection
    - DNS Spoofing
    - Weak Device Password

2. **Warning Risks (LaunchWithWarning)**:
    - USB Debugging enabled
    - Unknown apps installation allowed
    - Unencrypted WiFi
    - WiFi without password protection

#### Risk Status

Each risk has a status that can be:
- `RISK_STATUS_SAFE`: No security concerns
- `RISK_STATUS_WARN`: Warning level security concern
- `RISK_STATUS_UNSAFE`: Critical security concern

#### Implementation Example

```kotlin
// In your ViewModel
private fun checkStartupRisks() {
    viewModelScope.launch {
        try {
            val startupRisk = moneyGuardPrelaunch?.startup()
            
            if (startupRisk == null || startupRisk.risks.isEmpty()) {
                // No risks detected, proceed normally
            } else {
                val severeRisks = startupRisk.risks.filter { it.status == RiskStatus.RISK_STATUS_UNSAFE }
                val warningRisks = startupRisk.risks.filter { it.status == RiskStatus.RISK_STATUS_WARN }

                when (startupRisk.preLaunchVerdict.decision) {
                    PreLaunchDecision.DoNotLaunch -> {
                        // Show severe risk warning
                    }
                    PreLaunchDecision.LaunchWithWarning -> {
                        // Show warning risk message
                    }
                    PreLaunchDecision.Launch -> {
                        // Proceed normally
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}
```

#### Security Recommendations

Based on the risk assessment, the SDK provides specific recommendations for each risk type:

- For Root/Jailbreak: Advise against logging into banking apps
- For USB Debugging: Recommend disabling USB debugging
- For Weak Passwords: Recommend setting a strong device password
- For Unknown Apps: Recommend disabling installation from unknown sources
- For DNS Spoofing: Advise against proceeding with banking activities
- For WiFi Security: Recommend using encrypted and password-protected WiFi

## Error Handling

The SDK uses Kotlin's Result type for error handling. Always handle potential errors when making SDK calls:



### Authentication

```kotlin
// Using Flow
sdkService.authentication().register(partnerBankId, sessionToken)
    .collect { result ->
        when (result) {
            is MoneyGuardResult.Success -> {
                // Handle successful registration
            }
            is MoneyGuardResult.Failure -> {
                // Handle error
            }
            is MoneyGuardResult.Loading -> {
                // Handle loading state
            }
        }
    }

// Using Callback
sdkService.authentication().register(partnerBankId, sessionToken) { result ->
    when (result) {
        is MoneyGuardResult.Success -> {
            // Handle successful registration
        }
        is MoneyGuardResult.Failure -> {
            // Handle error
        }
        is MoneyGuardResult.Loading -> {
            // Handle loading state
        }
    }
}
```

#### Logout

The `logout()` function allows you to securely terminate the current session with the MoneyGuard service.

```kotlin
// Initialize the MoneyGuard SDK
val moneyGuardAuth = sdkService.authentication()

// Call logout when needed
moneyGuardAuth.logout()
```

### Credential Checking

The SDK provides two ways to check credentials for potential security risks:

#### 1. Using Callback

```kotlin
moneyGuardAuth.credentialCheck(
    sessionToken = "your-session-token",
    credential = Credential(
        username = "user@example.com",
        passwordStartingCharactersHash = "hashedPassword",
        hashAlgorithm = "SHA-256",
        domain = "example.com"
    ),
    onResult = { result ->
        when (result) {
            is MoneyGuardResult.Success -> {
                val scanResult = result.data
                // Handle successful credential check
            }
            is MoneyGuardResult.Failure -> {
                // Handle error
            }
            is MoneyGuardResult.Loading -> {
                // Handle loading state
            }
        }
    }
)
```

#### 2. Using Flow (Coroutines)

```kotlin
// In a coroutine scope
launch {
    moneyGuardAuth.credentialCheck(
        sessionToken = "your-session-token",
        credential = Credential(
            username = "user@example",
            passwordStartingCharactersHash = "hashedPassword",
            hashAlgorithm = "SHA-256",
            domain = "example.com"
        )
    ).collect { result ->
        when (result) {
            is MoneyGuardResult.Success -> {
                val scanResult = result.data
                // Handle successful credential check
            }
            is MoneyGuardResult.Failure -> {
                // Handle error
            }
            is MoneyGuardResult.Loading -> {
                // Handle loading state
            }
        }
    }
}
```

### MoneyGuardPolicy

The `MoneyGuardPolicy` interface provides methods for managing insurance policies and coverage limits. Here's a detailed breakdown of available operations:

#### Get Coverage Limits

```kotlin
suspend fun getCoverageLimits(token: String): Result<CoverageLimitResponse>
```

Retrieves available coverage limits for insurance policies.

**Parameters:**
- `token` (String): JWT token for authorization

**Returns:**
- `Result<CoverageLimitResponse>`: Contains list of coverage limits or error

#### Get Policy Options

```kotlin
suspend fun getPolicyOptions(token: String, coverageLimitId: Int): Result<PolicyOptionResponse>
```

Retrieves policy options for a specific coverage limit.

**Parameters:**
- `token` (String): JWT token for authorization
- `coverageLimitId` (Int): ID of the coverage limit to get options for

**Returns:**
- `Result<PolicyOptionResponse>`: Contains list of policy options or error

#### Get User Accounts

```kotlin
suspend fun getUserAccounts(token: String, partnerBankId: Int): Result<UserAccountsResponse>
```

Retrieves user's bank accounts for a specific partner bank.

**Parameters:**
- `token` (String): JWT token for authorization
- `partnerBankId` (Int): ID of the partner bank

**Returns:**
- `Result<UserAccountsResponse>`: Contains list of bank accounts or error

#### Create Policy

```kotlin
suspend fun createPolicy(
    token: String,
    policyOptionId: String,
    coveredAccountIds: List<String>,
    debitAccountId: String,
    autoRenew: Boolean
): Result<CreatePolicyResponse>
```

Creates a new insurance policy with specified parameters.

**Parameters:**
- `token` (String): JWT token for authorization
- `policyOptionId` (String): ID of the selected policy option
- `coveredAccountIds` (List<String>): List of account IDs to be covered by the policy
- `debitAccountId` (String): ID of the account to debit premium from
- `autoRenew` (Boolean): Whether the policy should auto-renew

**Returns:**
- `Result<CreatePolicyResponse>`: Contains created policy details or error

## Usage Example

```kotlin
// Initialize the policy service
val moneyGuardPolicy = sdkService().policy()

// Get coverage limits
val coverageLimits = moneyGuardPolicy.getCoverageLimits("your-jwt-token")

// Get policy options for a specific coverage limit
val policyOptions = moneyGuardPolicy.getPolicyOptions("your-jwt-token", coverageLimitId = 1)

// Get user accounts
val userAccounts = moneyGuardPolicy.getUserAccounts("your-jwt-token", partnerBankId = 1)

// Create a new policy
val newPolicy = moneyGuardPolicy.createPolicy(
    token = "your-jwt-token",
    policyOptionId = "option-123",
    coveredAccountIds = listOf("account-1", "account-2"),
    debitAccountId = "debit-account-1",
    autoRenew = true
)
```

## Error Handling

All API methods return a `Result` type that can contain either a successful response or an error. Handle the results appropriately in your application:

```kotlin
when (val result = moneyGuardPolicy.getCoverageLimits(token)) {
    is Result.Success -> {
        // Handle successful response
        val coverageLimits = result.getOrNull()
    }
    is Result.Failure -> {
        // Handle error
        val error = result.exceptionOrNull()
    }
}
```


### Claims Management

```kotlin
// Create a claim object
val claim = Claim(
    accountId = 1234567890L,  // The account ID where the loss occurred
    lossDate = Date(),        // The date when the loss occurred
    nameOfIncident = "Theft", // The type of incident, the values here are gotten from the incident list SDK function. 
    lossAmount = 5000.0,      // The amount lost in the incident
    statement = "Detailed description of the incident" // A detailed statement about the incident
)

// Create attachments
val attachments = listOf(
    // Create a file part from a file
    MultipartBody.Part.createFormData(
        "file",
        file.name,
        file.asRequestBody("image/*".toMediaType())
    )
)

// Submit a claim using suspend function
val claimResult = sdkService.claim().submitClaim(
    sessionToken = token,
    claim = claim,
    attachments = attachments
)

// Submit a claim using callback
sdkService.claim().submitClaim(
    sessionToken = token,
    claim = claim,
    attachments = attachments,
    onSuccess = { response ->
        // Handle successful claim submission
        println("Claim submitted successfully: ${response.message}")
    },
    onFailure = { error ->
        // Handle error
        println("Failed to submit claim: ${error.message}")
    }
)

// Get claims history using suspend function
val claims = sdkService.claim().getClaims(
    sessionToken = token,
    from = startDate,
    to = endDate,
    bank = bankName,
    claimStatus = ClaimStatus.ALL
)

// Get claims history using callback
sdkService.claim().getClaims(
    sessionToken = token,
    from = startDate,
    to = endDate,
    bank = bankName,
    claimStatus = ClaimStatus.ALL,
    onSuccess = { claims ->
        // Handle successful claims retrieval
        claims.forEach { claim ->
            println("Claim ID: ${claim.id}, Status: ${claim.status}")
        }
    },
    onFailure = { error ->
        // Handle error
        println("Failed to get claims: ${error.message}")
    }
)

// Get a specific claim using suspend function
val specificClaim = sdkService.claim().getClaim(
    sessionToken = token,
    claimId = 123
)

// Get a specific claim using callback
sdkService.claim().getClaim(
    sessionToken = token,
    claimId = 123,
    onSuccess = { claim ->
        // Handle successful claim retrieval
        println("Claim details: ${claim.brief}")
    },
    onFailure = { error ->
        // Handle error
        println("Failed to get claim: ${error.message}")
    }
)

// Get available incident types using suspend function
val incidentNames = sdkService.claim().getIncidentNames(token)

// Get available incident types using callback
sdkService.claim().getIncidentNames(
    sessionToken = token,
    onSuccess = { names ->
        // Handle successful incident names retrieval
        names.forEach { name ->
            println("Available incident: $name")
        }
    },
    onFailure = { error ->
        // Handle error
        println("Failed to get incident names: ${error.message}")
    }
)
```

#### API Access Methods

The SDK provides two ways to access the claims API:

1. **Suspend Functions (Coroutines)**
   - Use these in coroutine contexts
   - Better for structured concurrency
   - Easier error handling with try-catch
   - Example:
   ```kotlin
   lifecycleScope.launch {
       try {
           val result = sdkService.claim().submitClaim(...)
           // Handle success
       } catch (e: Exception) {
           // Handle error
       }
   }
   ```

2. **Callback-based Functions**
   - Use these in non-coroutine contexts
   - Traditional callback pattern
   - Separate success and error callbacks
   - Example:
   ```kotlin
   sdkService.claim().submitClaim(
       sessionToken = token,
       claim = claim,
       attachments = attachments,
       onSuccess = { response -> /* Handle success */ },
       onFailure = { error -> /* Handle error */ }
   )
   ```

Choose the method that best fits your application's architecture and requirements. The suspend functions are recommended for modern Kotlin applications using coroutines, while the callback-based approach is useful for legacy code or when working with non-coroutine contexts.

#### Claim Object Structure

The `Claim` object requires the following fields:

- `accountId` (Long): The ID of the account where the loss occurred
- `lossDate` (Date): The date when the incident occurred
- `nameOfIncident` (String): The type of incident (e.g., "Theft", "Fraud", "Loss")
- `lossAmount` (Double): The monetary amount lost in the incident
- `statement` (String): A detailed description of the incident

#### Supported Attachment Types

The SDK supports the following file types for claim attachments:

- Images: JPG, JPEG, PNG
- Documents: PDF
- Maximum file size: 10MB per file
- Maximum number of attachments: 5 files per claim

To create attachments, use `MultipartBody.Part` with the appropriate media type:

```kotlin
// For images
val imagePart = MultipartBody.Part.createFormData(
    "file",
    imageFile.name,
    imageFile.asRequestBody("image/*".toMediaType())
)

// For PDF documents
val pdfPart = MultipartBody.Part.createFormData(
    "file",
    pdfFile.name,
    pdfFile.asRequestBody("application/pdf".toMediaType())
)
```

#### Claim Status

Claims can have the following statuses:

- `Submitted`: Initial state when claim is submitted
- `UnderReview`: Claim is being reviewed
- `Verified`: Claim has been verified
- `Rejected`: Claim has been rejected
- `ProcessingPayment`: Payment is being processed
- `PaymentSent`: Payment has been sent
- `ReimbursementComplete`: Reimbursement process is complete

### Transaction Security

The SDK provides monitoring and security checks to protect your users' financial transactions. This feature helps detect and prevent fraudulent activities.

```kotlin
// Check debit transaction using coroutine suspend function
val result = sdkService.transactionCheck().checkDebitTransaction(
    sessionToken = token,
    debitTransaction = transaction
)

// Using callback
transactionCheck?.checkDebitTransaction(sessionToken, debitTransaction,
    onSuccess = { result ->
        if (result.success) {
            when (result.status) {
                RiskStatus.RISK_STATUS_WARN -> {
                    //handle event
                }
                RiskStatus.RISK_STATUS_UNSAFE_CREDENTIALS -> {
                    //handle event
                }
                RiskStatus.RISK_STATUS_UNSAFE_LOCATION -> {
                    //handle event
                }
                RiskStatus.RISK_STATUS_UNSAFE -> {
                    //handle event
                }
                else -> {

                }
            }
        }
    },
    onFailure = {

    }
)
```

#### Transaction Check Parameters

The `checkDebitTransaction` method accepts the following parameters:

- `sessionToken` (String): The user's session token
- `debitTransaction` (DebitTransaction): An object containing transaction details:
  - `sourceAccountNumber` (String): The source account number
  - `amount` (Double): The transaction amount
  - `memo` (String): The transaction description
  - `destinationBank` (String): The destination bank name
  - `destinationAccountNumber` (String): The destination account number
  - `location` (LatLng): The location (latitude and longitude) that the transaction occurred
    - `latitude` (Double)
    - `longitude` (Double)


#### Transaction Check Results

The check returns one of the following results:

- `RiskStatus.RISK_STATUS_WARN`
- `RiskStatus.RISK_STATUS_UNSAFE_CREDENTIALS`
- `RiskStatus.RISK_STATUS_UNSAFE_LOCATION`
- `RiskStatus.RISK_STATUS_UNSAFE`
- `RiskStatus.RISK_STATUS_UNSAFE_CREDENTIALS`
- `RiskStatus.RISK_STATUS_UNSAFE_LOCATION`


```kotlin
try {
    val result = sdkService.policy().getCoverageLimits(token)
    result.onSuccess { coverageLimits ->
        // Handle success
    }.onFailure { error ->
        // Handle error
    }
} catch (e: Exception) {
    // Handle unexpected errors
}
```


### TypingProfile

#### Overview

The `TypingProfile` interface provides biometric authentication capabilities through typing pattern recognition within the MoneyGuard SDK ecosystem. It leverages TypingDNA technology to capture and analyze unique typing characteristics for user authentication and fraud detection.

#### Key Features

- **Biometric Authentication**: Captures unique typing patterns as a biometric identifier
- **Real-time Recording**: Monitors typing behavior across specified UI targets
- **Pattern Matching**: Compares captured patterns against stored user profiles
- **Lifecycle Management**: Provides comprehensive service lifecycle control
- **Fraud Detection**: Integrates with MoneyGuard's broader security framework


#### API Reference

#### startService()

```kotlin
suspend fun startService(activity: Activity, targets: IntArray)
```

**Description**: Initializes and starts the typing pattern recording service for specified UI targets.

**Parameters**:
- `activity`: The Android activity context required for TypingDNA initialization. Must be a valid, non-finishing activity.
- `targets`: Array of view IDs to monitor for typing patterns. Must not be empty. Each ID should correspond to a text input view.

**Behavior**:
- Creates and initializes a TypingDNA recorder instance
- Registers the provided target view IDs for monitoring
- Begins real-time typing pattern capture
- Maintains service state to prevent duplicate initialization

**Target Views**:
The targets parameter accepts view IDs (from findViewById) that should be monitored:
- EditText fields (most common)
- Any text input components
- Multiple targets can be monitored simultaneously
---

#### pauseService()

```kotlin
fun pauseService()
```

**Description**: Temporarily pauses typing pattern recording without stopping the service.

**Use Cases**:
- Temporarily disable recording during sensitive operations

---

#### stopService()

```kotlin
fun stopService()
```

**Description**: Completely stops the typing pattern recording service and clears all state.

**Behavior**:
- Terminates TypingDNA recorder

---

#### resumeService()

```kotlin
fun resumeService()
```

**Description**: Resumes typing pattern recording after a pause operation.

**Behavior**:
- Restarts TypingDNA recorder

---

#### resetService()

```kotlin
fun resetService()
```

**Description**: Resets the typing pattern recording service while keeping it active.

**Behavior**:
- Clears typing history buffer

---

#### getTypingPattern()

```kotlin
fun getTypingPattern(text: String): String
```

**Description**: Extracts typing pattern data from the provided text input.

**Parameters**:
- `text`: The text input for which to extract typing patterns. Should match text typed in monitored target views.

**Returns**: Encoded typing pattern string ready for authentication, or empty string if pattern cannot be generated.

---

#### matchTypingProfile()

```kotlin
suspend fun matchTypingProfile(text: String, token: String): TypingProfileResult
```

**Description**: Matches captured typing patterns against stored user profiles for authentication.

**Parameters**:
- `text`: The text input used for pattern extraction. Should match text typed in monitored target views.
- `token`: Valid authentication token for API access. Must have appropriate permissions for typing profile operations.

**Returns**: `TypingProfileResult` containing authentication results and metadata. Includes success status, match results, confidence levels, and recommended actions.

#### Data Models

#### TypingProfileResult

```kotlin
data class TypingProfileResult(
    val success: Boolean,
    val message: String,
    val isEnrolledOnThisDevice: Boolean,
    val matched: Boolean,
    val highConfidence: Boolean,
    val tDnaStatus: Int,
    val message_Code: Int,
    val action: String,
    val enrollment: Int,
    val result: Int,
    val high_Confidence: Int,
    val asString: String,
)
```

**Fields**:
- `matchingIdentity`: User identity for pattern matching (typically empty)
- `typingPattern`: Encoded typing pattern data

#### Usage Examples
The `TypingProfile` interface is accessible through the main MoneyGuard SDK service:

```kotlin
val sdk = MoneyGuardSdk.initialize(this)
val typingProfile = sdk.getTypingProfile()
```

#### Basic Setup & Usage


```kotlin
class LoginActivity : AppCompatActivity() {
    private lateinit var typingProfile: TypingProfile
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Get TypingProfile instance from MoneyGuard SDK
        typingProfile = MoneyGuardSdk.getService().getTypingProfile()
        
        // Start monitoring typing on username and password fields
        val targets = intArrayOf(
            R.id.edittext_username,
            R.id.edittext_password
        )
        
        lifecycleScope.launch {
            typingProfile.startService(this@LoginActivity, targets)
        }
        
        
        // After typing
        val combinedText = "$username$password"

        // Match against stored profiles
        val result = typingProfile.matchTypingProfile(combinedText, authToken)
    }
}
```

## Requirements

- Android API level 21 or higher
- Kotlin 1.5.0 or higher
- AndroidX libraries

## Support

For support and questions, please contact:
- Email: 
- Website: 

## License

This SDK is proprietary software. All rights reserved. 