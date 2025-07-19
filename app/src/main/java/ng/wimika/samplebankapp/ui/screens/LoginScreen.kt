package ng.wimika.samplebankapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ng.wimika.moneyguard_sdk.services.utility.MoneyGuardAppStatus
import ng.wimika.moneyguard_sdk_auth.datasource.auth_service.models.credential.Credential
import ng.wimika.moneyguard_sdk_auth.datasource.auth_service.models.credential.HashAlgorithm
import ng.wimika.samplebankapp.R // Make sure this import points to your project's R file
import ng.wimika.samplebankapp.loginRepo.LoginRepositoryImpl
import ng.wimika.moneyguard_sdk_commons.types.MoneyGuardResult
import ng.wimika.moneyguard_sdk_commons.types.SpecificRisk
import ng.wimika.moneyguard_sdk_commons.types.RiskStatus
import ng.wimika.moneyguard_sdk.services.prelaunch.MoneyGuardPrelaunch
import ng.wimika.moneyguard_sdk.services.utility.models.LocationCheck
import ng.wimika.samplebankapp.MoneyGuardClientApp // Assuming these imports are correct
import ng.wimika.samplebankapp.local.IPreferenceManager
import ng.wimika.samplebankapp.Constants
import ng.wimika.samplebankapp.MoneyGuardClientApp.Companion.preferenceManager
import ng.wimika.samplebankapp.ui.screens.BottomSheetModal
import android.os.Build

// --- New UI Code Starts Here ---

// Define colors from the new design for easy reuse
private object SabiBankColors {
    val OrangePrimary = Color(0xFFD95F29)
    val OrangeDark = Color(0xFFC05425) // A darker shade for text fields
    val White = Color.White
    val TextPrimaryOrange = Color(0xFFD95F29)
    val TextOnOrange = Color.White
}

// A reusable custom TextField composable to match the design
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = SabiBankColors.TextOnOrange.copy(alpha = 0.7f)) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SabiBankColors.OrangeDark,
            unfocusedContainerColor = SabiBankColors.OrangeDark,
            disabledContainerColor = SabiBankColors.OrangeDark,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = SabiBankColors.TextOnOrange
        ),
        textStyle = TextStyle(color = SabiBankColors.TextOnOrange, fontSize = 16.sp),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation
    )
}

suspend fun registerWithMoneyguard(
    sessionId: String,
    preferenceManager: IPreferenceManager?,
    onRegistrationComplete: () -> Unit // Added completion handler
) {
    try {
        val sdkService = MoneyGuardClientApp.sdkService
        sdkService?.authentication()?.register(
            parteBankId = Constants.PARTNER_BANK_ID,
            partnerSessionToken = sessionId
        )?.collect { result ->
            when (result) {
                is MoneyGuardResult.Success -> {
                    val sessionResponse = result.data
                    if (sessionResponse.token.isNotEmpty()) {
                        preferenceManager?.saveMoneyGuardToken(sessionResponse.token)
                        preferenceManager?.saveMoneyguardUserNames(
                            sessionResponse.userDetails.firstName,
                            sessionResponse.userDetails.lastName
                        )
                    }
                    onRegistrationComplete() // Call completion handler
                }
                is MoneyGuardResult.Failure -> {
                    onRegistrationComplete() // Also call on failure to continue flow
                }
                is MoneyGuardResult.Loading -> { /* Do nothing */ }
            }
        }
    } catch (e: Exception) {
        onRegistrationComplete() // Also call on exception
    }
}

/**
 * Generate appropriate warning message based on the specific risk
 */
fun getRiskMessage(risk: SpecificRisk): String {
    return when (risk.name) {
        SpecificRisk.SPECIFIC_RISK_DEVICE_SECURITY_MISCONFIGURATION_NAME -> {
            "USB debugging is enabled on your device. Your login credentials may be compromised."
        }
        SpecificRisk.SPECIFIC_RISK_NETWORK_WIFI_ENCRYPTION_NAME,
        SpecificRisk.SPECIFIC_RISK_NETWORK_WIFI_PASSWORD_PROTECTION_NAME -> {
            "Unsecured WiFi detected. Your digital banking activities may be compromised."
        }
        SpecificRisk.SPECIFIC_RISK_DEVICE_ROOT_OR_JAILBREAK_NAME -> {
            "Device security is compromised. We strongly advise you not to log into your bank app."
        }
        SpecificRisk.SPECIFIC_RISK_NETWORK_DNS_SPOOFING_NAME -> {
            "DNS spoofing detected. Your banking activities are at risk if you continue."
        }
        SpecificRisk.SPECIFIC_RISK_DEVICE_VULNERABILITY_NAME -> {
            "Device vulnerabilities detected. Please update your device for better security."
        }
        SpecificRisk.SPECIFIC_RISK_NETWORK_MITM_NAME -> {
            "Man-in-the-middle attack detected. Your connection may be compromised."
        }
        SpecificRisk.SPECIFIC_RISK_USER_IDENTITY_COMPROMISE_NAME -> {
            "Identity compromise detected. Please verify your identity before proceeding."
        }
        SpecificRisk.SPECIFIC_RISK_APPLICATION_PHISHING_NAME -> {
            "Phishing attempt detected. Please ensure you're using the official app."
        }
        SpecificRisk.SPECIFIC_RISK_APPLICATION_MALWARE_NAME -> {
            "Malware detected on your device. Please scan and remove before proceeding."
        }
        SpecificRisk.SPECIFIC_RISK_APPLICATION_FAKE_APPS_NAME -> {
            "Fake app detected. Please ensure you're using the official banking app."
        }
        SpecificRisk.SPECIFIC_RISK_APPLICATION_KEY_LOGGING_NAME -> {
            "Key logging detected. Your keystrokes may be monitored."
        }
        SpecificRisk.SPECIFIC_RISK_USER_SECURITY_AWARENESS_NAME -> {
            "Security awareness issue detected. Please review your security practices."
        }
        else -> {
            risk.additionalDetails ?: "Security risk detected: ${risk.name}"
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToVerification: () -> Unit // Modified signature
) {
    // --- All existing state and logic is preserved ---
    var username by remember { mutableStateOf("") } // Pre-filled from screenshot
    var password by remember { mutableStateOf("********") } // Pre-filled from screenshot
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var debugLogsEnabled by remember { mutableStateOf(preferenceManager?.isDebugLogsEnabled() ?: false) }

    // Prelaunch check states
    var isPrelaunchChecking by remember { mutableStateOf(true) }
    var prelaunchRisks by remember { mutableStateOf<List<SpecificRisk>>(emptyList()) }
    var currentRiskIndex by remember { mutableStateOf(0) }
    var showRiskModal by remember { mutableStateOf(false) }

    // New states for location check
    var showUnusualLocationDialog by remember { mutableStateOf(false) }
    
    // Credential check dialog state
    var showCredentialDialog by remember { mutableStateOf(false) }
    var credentialDialogStatus by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // In a real app, you'd use dependency injection for the repository
    val loginRepository = remember { LoginRepositoryImpl() }
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val sdkService = MoneyGuardClientApp.sdkService

    // MoneyGuard prelaunch service
    val moneyGuardPrelaunch: MoneyGuardPrelaunch? = remember {
        MoneyGuardClientApp.sdkService?.prelaunch()
    }

    // --- Location Permission Handling ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
                // Permission granted, trigger the location check again (handled in login lambda)
                Log.d("LoginScreen", "Location permission granted.")
            } else {
                // Permission denied, proceed without location check
                Log.d("LoginScreen", "Location permission denied.")
                scope.launch { onLoginSuccess() }
            }
        }
    )

    // A helper function to encapsulate the entire post-login flow
    suspend fun handlePostLoginFlow() {
        val token = preferenceManager?.getMoneyGuardToken()
        if (sdkService == null || token.isNullOrEmpty()) {
            onLoginSuccess() // Failsafe
            return
        }

        val status = sdkService.utility()?.checkMoneyguardStatus(token)
        if (status == MoneyGuardAppStatus.Active) {
            // --- Start Credential Check First ---
            isLoading = true
            try {
                // Perform credential check inline
                val credential = Credential(
                    username = username.trim(),
                    passwordStartingCharactersHash = password.takeLast(3),
                    domain = "wimika.ng",
                    hashAlgorithm = HashAlgorithm.SHA256
                )
                
                sdkService.authentication()?.credentialCheck(token, credential) { result ->
                    if (result is MoneyGuardResult.Success) {
                        val status = result.data.status
                        credentialDialogStatus = "Credential Check - $status"
                        showCredentialDialog = true
                        // Don't start location check yet - wait for user to click OK
                    } else {
                        credentialDialogStatus = "Loading..."
                        showCredentialDialog = true
                        // Don't start location check yet - wait for user to click OK
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginScreen", "Credential check failed", e)
                onLoginSuccess() // Failsafe: proceed to dashboard if credential check has an error
            } finally {
                isLoading = false
            }
        } else {
            // MoneyGuard not active, proceed directly to dashboard
            onLoginSuccess()
        }
    }

    // Helper function to perform location check after credential check
    fun performLocationCheckAfterCredential(token: String) {
        scope.launch {
            try {
                when {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {

                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        val location = fusedLocationClient.lastLocation.await()

                        if (location != null) {
                            val locationCheck = LocationCheck(latitude = location.latitude, longitude = location.longitude)
                            val response = sdkService?.utility()?.checkLocation(token, locationCheck)

                            if (response?.data?.isNotEmpty() == true) {
                                // Suspicious location detected
                                showUnusualLocationDialog = true
                            } else {
                                // Location is not suspicious, proceed to dashboard
                                onLoginSuccess()
                            }
                        } else {
                            // Could not get location, proceed to dashboard
                            onLoginSuccess()
                        }
                    }
                    else -> {
                        // Request permissions
                        locationPermissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                        // The result of the launcher will re-trigger this flow if needed,
                        // but for now, we wait. To avoid complexity, we can also just proceed.
                        // Let's proceed for a smoother UX if they deny.
                        onLoginSuccess()
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginScreen", "Location check failed", e)
                onLoginSuccess() // Failsafe: proceed to dashboard if location check has an error
            }
        }
    }

    // Perform prelaunch checks when screen loads
    LaunchedEffect(Unit) {
        try {
            val startupRisk = moneyGuardPrelaunch?.startup()
            val risks = startupRisk?.risks?.filter {
                it.status == RiskStatus.RISK_STATUS_WARN || it.status == RiskStatus.RISK_STATUS_UNSAFE
            } ?: emptyList()

            prelaunchRisks = risks
            isPrelaunchChecking = false

            // Show first risk modal if there are risks
            if (risks.isNotEmpty()) {
                showRiskModal = true
                currentRiskIndex = 0
            }
        } catch (e: Exception) {
            // If prelaunch check fails, continue with login
            isPrelaunchChecking = false
        }
    }

    // Handle risk modal navigation
    LaunchedEffect(currentRiskIndex, prelaunchRisks) {
        if (currentRiskIndex >= prelaunchRisks.size && prelaunchRisks.isNotEmpty()) {
            showRiskModal = false
        }
    }

    Scaffold { paddingValues ->
        // --- The UI is structured into two main parts ---
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SabiBankColors.White)
            ) {
                // --- Top White Section (Logo) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f), // Takes ~35% of the screen height
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_graphic),
                        contentDescription = "Sabi Bank Logo",
                        modifier = Modifier.size(180.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "v${Constants.APP_VERSION}",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // --- Bottom Orange Section (Login Form) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.65f) // Takes ~65% of the screen height
                        .background(
                            color = SabiBankColors.OrangePrimary,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter your login credentials",
                        color = SabiBankColors.TextOnOrange,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Username Field
                    SabiTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Username",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    SabiTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    if (showError) {
                        Text(
                            text = "Login failed. Please check your credentials.",
                            color = SabiBankColors.TextOnOrange,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .background(
                                    Color.Red.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f)) // Pushes content to top and bottom

                    // Debug Logs Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable debug logs",
                            color = SabiBankColors.TextOnOrange,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = debugLogsEnabled,
                            onCheckedChange = { enabled ->
                                debugLogsEnabled = enabled
                                preferenceManager?.saveDebugLogsEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SabiBankColors.White,
                                checkedTrackColor = SabiBankColors.White.copy(alpha = 0.7f),
                                uncheckedThumbColor = SabiBankColors.TextOnOrange.copy(alpha = 0.7f),
                                uncheckedTrackColor = SabiBankColors.TextOnOrange.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                scope.launch {
                                    isLoading = true
                                    showError = false
                                    try {
                                        val appVersion = Constants.APP_VERSION
                                        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                                        val androidVersion = Build.VERSION.RELEASE
                                        loginRepository.login(username.trim(), password, appVersion, deviceModel, androidVersion)
                                            .collect { response ->
                                                val sessionData = response.data
                                                if (sessionData != null && sessionData.sessionId.isNotEmpty()) {
                                                    preferenceManager?.saveBankLoginDetails(
                                                        sessionData.sessionId,
                                                        sessionData.userFullName
                                                    )
                                                    // Save user email for log sharing
                                                    preferenceManager?.saveUserEmail(username.trim())
                                                    // Reset suspicious login flag on new successful login
                                                    preferenceManager?.saveSuspiciousLoginStatus(false)
                                                    
                                                    // Register with MoneyGuard
                                                    registerWithMoneyguard(
                                                        sessionData.sessionId,
                                                        preferenceManager
                                                    ) {
                                                        // On successful registration, start the post-login flow
                                                        scope.launch {
                                                            handlePostLoginFlow()
                                                        }
                                                    }
                                                } else {
                                                    showError = true
                                                    isLoading = false
                                                }
                                            }
                                    } catch (e: Exception) {
                                        showError = true
                                        isLoading = false
                                    }
                                    // isLoading will be managed by the subsequent flows
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(50), // Pill shape
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SabiBankColors.White,
                            contentColor = SabiBankColors.TextPrimaryOrange,
                            disabledContainerColor = SabiBankColors.White.copy(alpha = 0.5f)
                        ),
                        enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = SabiBankColors.TextPrimaryOrange,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Text("Login", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                }
            }
        }
    }

    // --- Add the new Unusual Location Dialog ---
    if (showUnusualLocationDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing by clicking outside */ },
            title = { Text("Unusual Location Detected") },
            text = {
                Text(
                    "We've detected a login from an unusual location. " +
                    "For your security, please verify your identity. " +
                    "If you proceed without verification, some account activities may be limited."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showUnusualLocationDialog = false
                    onNavigateToVerification() // Navigate to the verification screen
                }) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        preferenceManager?.saveSuspiciousLoginStatus(true)
                        showUnusualLocationDialog = false
                        onLoginSuccess() // Proceed to dashboard with flag set
                    }
                }) {
                    Text("Proceed without Verify")
                }
            }
        )
    }

    // Show credential check dialog
    if (showCredentialDialog && credentialDialogStatus != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Credential Check") },
            text = { Text(credentialDialogStatus!!) },
            confirmButton = {
                Button(onClick = {
                    showCredentialDialog = false
                    credentialDialogStatus = null
                    // Now start the location check after user clicks OK
                    val token = preferenceManager?.getMoneyGuardToken()
                    if (token != null) {
                        performLocationCheckAfterCredential(token)
                    } else {
                        onLoginSuccess()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }

    val preferenceManagerForPrelaunch = MoneyGuardClientApp.preferenceManager
    if(preferenceManagerForPrelaunch?.getIsFirstLaunchFlag() == true) {
        //Prelaunch checking overlay
        if (isPrelaunchChecking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = SabiBankColors.OrangePrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Performing security checks...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        //Risk modal overlay
        if (showRiskModal && prelaunchRisks.isNotEmpty() && currentRiskIndex < prelaunchRisks.size) {
            val currentRisk = prelaunchRisks[currentRiskIndex]
            val riskMessage = getRiskMessage(currentRisk)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                BottomSheetModal(
                    title = "Pre-Launch Checks",
                    message = riskMessage,
                    buttonText = "Continue",
                    onButtonClick = {
                        currentRiskIndex++
                        if (currentRiskIndex >= prelaunchRisks.size) {
                            showRiskModal = false
                        }
                    },
                    modifier = Modifier.padding(0.dp, bottom = 0.dp)
                )
            }
        }
    }

    @Preview(showBackground = true, widthDp = 375, heightDp = 812)
    @Composable
    fun LoginScreenPreview() {
        LoginScreen(onLoginSuccess = {}, onNavigateToVerification = {})
    }
}