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

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object OnboardingInfo : Screen()
    object AccountSelection : Screen()
    object CoverageLimitSelection : Screen()
    object PolicyOptionSelection : Screen()
    object Summary : Screen()
    object Checkout : Screen()
    object DownloadMoneyGuard : Screen()
    object CheckDebitTransaction : Screen()
    object EnrollmentIntro : Screen()
    object TypingPatternEnrollment : Screen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    
    // Check if user is already logged in
    LaunchedEffect(Unit) {
        val preferenceManager = ng.wimika.samplebankapp.MoneyGuardClientApp.preferenceManager
        val sessionId = preferenceManager?.getBankSessionId()
        if (!sessionId.isNullOrBlank()) {
            currentScreen = Screen.Dashboard
        }
    }

    when (currentScreen) {
        Screen.Login -> {
            LoginScreen(
                onLoginSuccess = {
                    currentScreen = Screen.Dashboard
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
                }
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
    }
} 