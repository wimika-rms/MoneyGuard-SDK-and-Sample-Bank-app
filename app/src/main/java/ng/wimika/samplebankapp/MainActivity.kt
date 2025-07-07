package ng.wimika.samplebankapp

import android.os.Bundle
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
        val preferenceManager =
            MoneyGuardClientApp.preferenceManager
        preferenceManager?.setIsFirstLaunchFlag();
        setContent {
            MoneyguardSampleBankAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation()
                }
            }
        }
    }
}