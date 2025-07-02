package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.local.MoneyGuardSetupPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val preferences = preferenceManager?.getMoneyGuardSetupPreferences() ?: MoneyGuardSetupPreferences()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Accounts Covered",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Display selected account IDs
                items(preferences.accountIds) { accountId ->
                    Text(
                        text = "Account: $accountId",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    Text(
                        text = "Amount Covered",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = preferences.amountToCover.ifEmpty { "Not selected" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    Text(
                        text = "Subscription Plan",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = preferences.subscriptionPlan.ifEmpty { "Not selected" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    Text(
                        text = "Auto-renew",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = if (preferences.autoRenew) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Checkout")
            }
        }
    }
} 