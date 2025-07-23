package ng.wimika.samplebankapp.ui.navigation

import androidx.compose.runtime.*
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

    when (val screen = currentScreen) { // Use 'screen' for smart casting
        Screen.Login -> {
            LoginScreen(
                onLoginSuccess = {
                    currentScreen = Screen.Dashboard
                },
                // New navigation action for verification
                onNavigateToVerification = {
                    currentScreen = Screen.TypingPatternVerification(onResult = { isSuccess ->
                        if (isSuccess) {
                            // On successful verification, proceed to dashboard
                            currentScreen = Screen.Dashboard
                        } else {
                            // On failure, return to the login screen
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
                onDownloadComplete = {
                    currentScreen = Screen.Dashboard
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
} 