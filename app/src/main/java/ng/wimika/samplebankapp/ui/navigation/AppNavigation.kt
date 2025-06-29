package ng.wimika.samplebankapp.ui.navigation

import androidx.compose.runtime.*
import ng.wimika.samplebankapp.ui.screens.DashboardScreen
import ng.wimika.samplebankapp.ui.screens.LoginScreen

@Composable
fun AppNavigation() {
    var isLoggedIn by remember { mutableStateOf(false) }
    
    // Check if user is already logged in
    LaunchedEffect(Unit) {
        val preferenceManager = ng.wimika.samplebankapp.MoneyGuardClientApp.preferenceManager
        val sessionId = preferenceManager?.getBankSessionId()
        isLoggedIn = !sessionId.isNullOrBlank()
    }

    if (isLoggedIn) {
        DashboardScreen(
            onLogout = {
                isLoggedIn = false
            }
        )
    } else {
        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
            }
        )
    }
} 