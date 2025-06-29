package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit
) {
    val preferenceManager = ng.wimika.samplebankapp.MoneyGuardClientApp.preferenceManager
    
    val userFullName = preferenceManager?.getBankUserFullName() ?: "User"
    val isMoneyguardEnabled = preferenceManager?.isMoneyguardEnabled() ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome, $userFullName!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Account Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = if (isMoneyguardEnabled) {
                        "Your account is protected by MoneyGuard"
                    } else {
                        "Your account is not protected"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = {
                // TODO: Implement button action
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = if (isMoneyguardEnabled) {
                    "Launch MoneyGuard"
                } else {
                    "Protect your account Now"
                }
            )
        }

        if (isMoneyguardEnabled) {
            val firstName = preferenceManager?.getMoneyguardFirstName()
            val lastName = preferenceManager?.getMoneyguardLastName()
            
            if (!firstName.isNullOrBlank() || !lastName.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "MoneyGuard Profile",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = "Name: ${firstName ?: ""} ${lastName ?: ""}".trim(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Logout button
        OutlinedButton(
            onClick = {
                // Clear all preferences
                preferenceManager?.clear()
                // Navigate back to login
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Logout")
        }
    }
} 