package ng.wimika.samplebankapp.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.util.Log
import ng.wimika.samplebankapp.ui.screens.DashboardScreen
import ng.wimika.samplebankapp.ui.screens.LoginScreen
import ng.wimika.samplebankapp.ui.screens.OnboardingInfoScreen
import ng.wimika.samplebankapp.ui.screens.AccountSelectionScreen
import ng.wimika.samplebankapp.ui.screens.CheckDebitScreen
import ng.wimika.samplebankapp.ui.screens.CoverageLimitSelectionScreen
import ng.wimika.samplebankapp.ui.screens.PolicyOptionSelectionScreen
import ng.wimika.samplebankapp.ui.screens.SummaryScreen
import ng.wimika.samplebankapp.ui.screens.CheckoutScreen
import ng.wimika.samplebankapp.ui.screens.DownloadMoneyGuardScreen
import ng.wimika.samplebankapp.ui.screens.EnrollmentIntroScreen
import ng.wimika.samplebankapp.ui.screens.TypingPatternScreen
import ng.wimika.samplebankapp.ui.screens.TypingPatternVerificationScreen
import ng.wimika.samplebankapp.ui.screens.claims.*
import ng.wimika.moneyguard_sdk_commons.types.MoneyGuardResult
import ng.wimika.moneyguard_sdk_auth.datasource.auth_service.models.TrustedDeviceRequest

sealed class Screen {
    data object Login : Screen()
    data object Dashboard : Screen()
    data object OnboardingInfo : Screen()
    data object AccountSelection : Screen()
    data object CoverageLimitSelection : Screen()
    data object PolicyOptionSelection : Screen()
    data object Summary : Screen()
    data object Checkout : Screen()
    data object DownloadMoneyGuard : Screen()
    data object CheckDebitTransaction : Screen()
    data object EnrollmentIntro : Screen()
    data object TypingPatternEnrollment : Screen()
    // Modified to carry a callback
    data class TypingPatternVerification(val onResult: (Boolean) -> Unit) : Screen()
    // Claims screens
    data object ClaimsList : Screen()
    data object SubmitClaim : Screen()
    data class ClaimsDetails(val claimId: Int) : Screen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var showTrustDeviceDialog by remember { mutableStateOf(false) }
    var isProcessingTrust by remember { mutableStateOf(false) }
    var trustResult by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Check if user is already logged in
    LaunchedEffect(Unit) {
        val preferenceManager = ng.wimika.samplebankapp.MoneyGuardClientApp.preferenceManager
        val sessionId = preferenceManager?.getBankSessionId()
        // Clear suspicious login flag on app start
        preferenceManager?.saveSuspiciousLoginStatus(false)
        if (!sessionId.isNullOrBlank()) {
            currentScreen = Screen.Dashboard
        }
    }
    
    // Function to handle device trust after successful verification
    fun handleSuccessfulVerification() {
        Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] 🔐 Starting device trust process after successful verification")
        scope.launch {
            isProcessingTrust = true
            showTrustDeviceDialog = true
            
            try {
                val preferenceManager = ng.wimika.samplebankapp.MoneyGuardClientApp.preferenceManager
                val sdkService = ng.wimika.samplebankapp.MoneyGuardClientApp.sdkService
                val token = preferenceManager?.getMoneyGuardToken()
                val sessionId = preferenceManager?.getBankSessionId()
                
                Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] Retrieved auth data - Token: ${if (token != null) "Present" else "Missing"}, SessionId: ${if (sessionId != null) "Present" else "Missing"}, SDK: ${if (sdkService != null) "Available" else "Unavailable"}")
                
                if (token != null && sessionId != null && sdkService != null) {
                    // Get the installation ID (device ID) from shared preferences
                    val sharedPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    val deviceId = sharedPrefs.getString("device_id", "") ?: ""
                    
                    Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] Device details - DeviceId: ${deviceId.take(8)}..., DeviceName: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    
                    val trustedDeviceRequest = TrustedDeviceRequest(
                        userId = sessionId, // Use session ID as user ID
                        installationId = deviceId,
                        deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                    )
                    
                    Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] 📡 Calling trustDevice API...")
                    sdkService.authentication()?.trustDevice(token, deviceId, trustedDeviceRequest)?.collect { result ->
                        when (result) {
                            is MoneyGuardResult.Success -> {
                                trustResult = "Device trusted successfully! You can now securely access your account on this device."
                                Log.i("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] ✅ Device trust SUCCESSFUL - Device is now trusted")
                            }
                            is MoneyGuardResult.Failure -> {
                                trustResult = "Failed to trust device: ${result.error.message}"
                                Log.e("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] ❌ Device trust FAILED: ${result.error.message}")
                            }
                            is MoneyGuardResult.Loading -> {
                                Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] ⏳ Device trust in progress...")
                            }
                        }
                    }
                } else {
                    trustResult = "Error: Missing authentication information"
                    Log.e("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] ❌ Missing required authentication data")
                }
            } catch (e: Exception) {
                trustResult = "Error trusting device: ${e.message}"
                Log.e("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] ❌ Exception during device trust: ${e.message}", e)
            } finally {
                isProcessingTrust = false
                Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] Device trust process completed")
            }
        }
    }
    
    // Function to logout user
    fun logoutUser() {
        Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] 🚪 Starting user logout process")
        val preferenceManager = ng.wimika.samplebankapp.MoneyGuardClientApp.preferenceManager
        val sdkService = ng.wimika.samplebankapp.MoneyGuardClientApp.sdkService
        
        // Clear all stored data
        preferenceManager?.saveBankLoginDetails("", "")
        preferenceManager?.saveMoneyGuardToken("")
        preferenceManager?.saveMoneyguardUserNames("", "")
        Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] Cleared all stored user data")
        
        // Call SDK logout
        sdkService?.authentication()?.logout()
        Log.d("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] Called SDK logout")
        
        Log.i("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] ✅ User logout completed - Returning to login screen")
    }

    when (val screen = currentScreen) { // Use 'screen' for smart casting
        Screen.Login -> {
            LoginScreen(
                onLoginSuccess = {
                    currentScreen = Screen.Dashboard
                },
                // New navigation action for verification
                onNavigateToVerification = {
                    Log.w("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] 🔄 Navigating to typing pattern verification screen")
                    currentScreen = Screen.TypingPatternVerification(onResult = { isSuccess ->
                        if (isSuccess) {
                            Log.i("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] ✅ Typing pattern verification SUCCESSFUL")
                            // On successful verification, trust the device
                            handleSuccessfulVerification()
                        } else {
                            Log.w("TRUSTED_DEVICE_FLOW", "[SampleBankApp|AppNavigation] ❌ Typing pattern verification FAILED - Logging out user")
                            // On failure, logout and return to login screen
                            logoutUser()
                            currentScreen = Screen.Login
                        }
                    })
                }
            )
        }
        Screen.Dashboard -> {
            DashboardScreen(
                onLogout = {
                    currentScreen = Screen.Login
                },
                onProtectAccount = {
                    currentScreen = Screen.OnboardingInfo
                },
                onDownloadMoneyGuard = {
                    currentScreen = Screen.DownloadMoneyGuard
                },
                onCheckDebitClick = {
                    currentScreen = Screen.CheckDebitTransaction
                },
                onEnrollTypingPattern = {
                    currentScreen = Screen.EnrollmentIntro
                },
                onVerifyTypingPattern = {
                    // Updated to use the new verification screen signature
                    currentScreen = Screen.TypingPatternVerification(onResult = {
                        // Simply return to dashboard regardless of result from here
                        currentScreen = Screen.Dashboard
                    })
                },
                onNavigateToClaims = {
                    currentScreen = Screen.ClaimsList
                }
            )
        }
        // Updated to handle the data class
        is Screen.TypingPatternVerification -> {
            TypingPatternVerificationScreen(
                onVerificationResult = screen.onResult
            )
        }
        Screen.OnboardingInfo -> {
            OnboardingInfoScreen(
                onGetStarted = {
                    currentScreen = Screen.AccountSelection
                },
                onLearnMore = { url ->
                    // This is now handled directly in the OnboardingInfoScreen
                },
                onBack = {
                    currentScreen = Screen.Dashboard
                }
            )
        }
        Screen.AccountSelection -> {
            AccountSelectionScreen(
                onBack = {
                    currentScreen = Screen.OnboardingInfo
                },
                onContinue = {
                    currentScreen = Screen.CoverageLimitSelection
                }
            )
        }
        Screen.CoverageLimitSelection -> {
            CoverageLimitSelectionScreen(
                onBack = {
                    currentScreen = Screen.AccountSelection
                },
                onContinue = {
                    currentScreen = Screen.PolicyOptionSelection
                }
            )
        }
        Screen.PolicyOptionSelection -> {
            PolicyOptionSelectionScreen(
                onBack = {
                    currentScreen = Screen.CoverageLimitSelection
                },
                onContinue = {
                    currentScreen = Screen.Summary
                }
            )
        }
        Screen.Summary -> {
            SummaryScreen(
                onBack = {
                    currentScreen = Screen.PolicyOptionSelection
                },
                onCheckout = {
                    currentScreen = Screen.Checkout
                }
            )
        }
        Screen.Checkout -> {
            CheckoutScreen(
                onBack = {
                    currentScreen = Screen.Summary
                },
                onProceed = {
                    currentScreen = Screen.DownloadMoneyGuard
                }
            )
        }
        Screen.DownloadMoneyGuard -> {
            DownloadMoneyGuardScreen(
                onBack = {
                    currentScreen = Screen.Dashboard
                },
                onDownloadComplete = {
                    currentScreen = Screen.Login
                }
            )
        }

        Screen.CheckDebitTransaction -> {
            CheckDebitScreen(
                onLocationPermissionDismissed = {
                    currentScreen = Screen.Dashboard
                },
                onBackClick = {
                    currentScreen = Screen.Dashboard
                }
            )
        }
        Screen.EnrollmentIntro -> {
            EnrollmentIntroScreen(
                onBack = {
                    currentScreen = Screen.Dashboard
                },
                onStartCapture = {
                    currentScreen = Screen.TypingPatternEnrollment
                }
            )
        }
        Screen.TypingPatternEnrollment -> {
            TypingPatternScreen(
                onBack = { currentScreen = Screen.Dashboard },
                onRegistrationComplete = { currentScreen = Screen.Dashboard }
            )
        }
        Screen.ClaimsList -> {
            ClaimsListScreen(
                onBackPressed = { 
                    currentScreen = Screen.Dashboard 
                },
                onClaimClick = { claimId ->
                    currentScreen = Screen.ClaimsDetails(claimId)
                },
                onSubmitNewClaim = {
                    currentScreen = Screen.SubmitClaim
                }
            )
        }
        Screen.SubmitClaim -> {
            SubmitClaimScreen(
                onBackPressed = {
                    currentScreen = Screen.ClaimsList
                }
            )
        }
        is Screen.ClaimsDetails -> {
            ClaimsDetailsScreen(
                claimId = screen.claimId,
                onBackPressed = {
                    currentScreen = Screen.ClaimsList
                }
            )
        }
    }
    
    // Trust Device Result Dialog
    if (showTrustDeviceDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing by clicking outside */ },
            title = { 
                Text(if (isProcessingTrust) "Processing..." else "Device Trust")
            },
            text = { 
                if (isProcessingTrust) {
                    Text("Trusting this device...")
                } else {
                    Text(trustResult ?: "Processing device trust...")
                }
            },
            confirmButton = {
                if (!isProcessingTrust && trustResult != null) {
                    Button(onClick = {
                        showTrustDeviceDialog = false
                        trustResult = null
                        // Logout user and return to login
                        logoutUser()
                        currentScreen = Screen.Login
                    }) {
                        Text("Continue")
                    }
                }
            },
            dismissButton = null
        )
    }
} 