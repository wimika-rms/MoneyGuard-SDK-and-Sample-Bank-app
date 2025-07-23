package ng.wimika.samplebankapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import ng.wimika.samplebankapp.ui.navigation.AppNavigation
import ng.wimika.samplebankapp.ui.theme.MoneyguardSampleBankAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferenceManager = MoneyGuardClientApp.preferenceManager
        
        // Mark that the app has started (for force-close detection)
        preferenceManager?.markAppStarted()
        preferenceManager?.saveLoggedOut(false)
        
        setContent {
            MoneyguardSampleBankAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Only clear preferences if the activity is finishing (being destroyed permanently)
        if (isFinishing) {
            Log.d("MainActivity", "App is finishing - clearing shared preferences")
            val preferenceManager = MoneyGuardClientApp.preferenceManager
            // Mark as properly closed before clearing
            preferenceManager?.markAppProperlyClosed()
            preferenceManager?.clearAllOnAppClose()
        }
    }
}