package ng.wimika.samplebankapp.ui.screens

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
import kotlinx.coroutines.launch
import ng.wimika.samplebankapp.R // Make sure this import points to your project's R file
import ng.wimika.samplebankapp.loginRepo.LoginRepositoryImpl
import ng.wimika.moneyguard_sdk_commons.types.MoneyGuardResult
import ng.wimika.moneyguard_sdk_commons.types.SpecificRisk
import ng.wimika.moneyguard_sdk_commons.types.RiskStatus
import ng.wimika.moneyguard_sdk.services.prelaunch.MoneyGuardPrelaunch
import ng.wimika.samplebankapp.MoneyGuardClientApp // Assuming these imports are correct
import ng.wimika.samplebankapp.local.IPreferenceManager
import ng.wimika.samplebankapp.Constants

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
    onLoginSuccess: () -> Unit
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
                    onLoginSuccess()
                }
                is MoneyGuardResult.Failure -> {
                    onLoginSuccess()
                }
                is MoneyGuardResult.Loading -> {
                    // Loading state - do nothing
                }
            }
        }
    } catch (e: Exception) {
        onLoginSuccess()
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
    onLoginSuccess: () -> Unit
) {
    // --- All existing state and logic is preserved ---
    var username by remember { mutableStateOf("") } // Pre-filled from screenshot
    var password by remember { mutableStateOf("********") } // Pre-filled from screenshot
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    // Prelaunch check states
    var isPrelaunchChecking by remember { mutableStateOf(true) }
    var prelaunchRisks by remember { mutableStateOf<List<SpecificRisk>>(emptyList()) }
    var currentRiskIndex by remember { mutableStateOf(0) }
    var showRiskModal by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    // In a real app, you'd use dependency injection for the repository
    val loginRepository = remember { LoginRepositoryImpl() }

    // MoneyGuard prelaunch service
    val moneyGuardPrelaunch: MoneyGuardPrelaunch? = remember {
        MoneyGuardClientApp.sdkService?.prelaunch()
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

    // --- The UI is structured into two main parts ---
    Box(modifier = Modifier.fillMaxSize()) {
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
//            Spacer(modifier = Modifier.height(8.dp))
//            Image(
//                painter = painterResource(id = R.drawable.logo_text),
//                contentDescription = "Sabi Bank",
//                modifier = Modifier.height(30.dp)
//            )
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

                // Forgot Password Link
//            TextButton(
//                onClick = { /* TODO: Handle Forgot Password */ },
//                modifier = Modifier.align(Alignment.End)
//            ) {
//                Text(
//                    text = "Forgot Password",
//                    color = SabiBankColors.TextOnOrange,
//                    fontSize = 14.sp
//                )
//            }

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

                // Login Button
                Button(
                    onClick = {
                        if (username.isNotBlank() && password.isNotBlank()) {
                            scope.launch {
                                isLoading = true
                                showError = false

                                try {
                                    loginRepository.login(username.trim(), password)
                                        .collect { response ->
                                            val sessionData = response.data
                                            if (sessionData != null && sessionData.sessionId.isNotEmpty()) {
                                                val preferenceManager =
                                                    MoneyGuardClientApp.preferenceManager
                                                preferenceManager?.saveBankLoginDetails(
                                                    sessionData.sessionId,
                                                    sessionData.userFullName
                                                )
                                                registerWithMoneyguard(
                                                    sessionData.sessionId,
                                                    preferenceManager,
                                                    onLoginSuccess
                                                )
                                            } else {
                                                showError = true
                                            }
                                        }
                                } catch (e: Exception) {
                                    showError = true
                                } finally {
                                    isLoading = false
                                }
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

                // Open an account Link
//            TextButton(onClick = { /* TODO: Handle Open Account */ }) {
//                Text(
//                    "Open an account",
//                    color = SabiBankColors.TextOnOrange,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
            }

            // Prelaunch checking overlay
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

            // Risk modal overlay
            if (showRiskModal && prelaunchRisks.isNotEmpty() && currentRiskIndex < prelaunchRisks.size) {
                val currentRisk = prelaunchRisks[currentRiskIndex]
                val riskMessage = getRiskMessage(currentRisk)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetModal(
                        title = "Security Warning",
                        message = riskMessage,
                        buttonText = "Continue",
                        onButtonClick = {
                            currentRiskIndex++
                            if (currentRiskIndex >= prelaunchRisks.size) {
                                showRiskModal = false
                            }
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    @Preview(showBackground = true, widthDp = 375, heightDp = 812)
    @Composable
    fun LoginScreenPreview() {
        LoginScreen(onLoginSuccess = {})
    }
}