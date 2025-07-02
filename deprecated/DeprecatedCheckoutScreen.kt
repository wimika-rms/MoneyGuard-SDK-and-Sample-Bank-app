package ng.wimika.moneyguardsdkclient.ui.features.moneyguard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    moneyGuardPolicy: MoneyGuardPolicy,
    token: String,
    onBack: () -> Unit,
    onProceed: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<BankAccount?>(null) }
    var autoRenew by remember { mutableStateOf(false) }
    var isCreatingPolicy by remember { mutableStateOf(false) }
    val viewModel: MoneyGuardViewModel = viewModel(
        factory = MoneyGuardViewModelFactory(moneyGuardPolicy, token)
    )
    val accounts by viewModel.bankAccounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Checkout",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Direct Account Debit Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Direct Account Debit",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Account Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.let { "${it.bank} - ${it.number} (${it.type})" } ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.bank} - ${account.number} (${account.type})") },
                                    onClick = {
                                        selectedAccount = account
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-renew Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-renew Policy",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = autoRenew,
                        onCheckedChange = { autoRenew = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Proceed Button
        Button(
            onClick = { 
                selectedAccount?.let { account ->
                    isCreatingPolicy = true
                    onProceed(account.id.toString(), autoRenew)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedAccount != null && !isCreatingPolicy
        ) {
            if (isCreatingPolicy) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Text("Purchasing Policy...")
                }
            } else {
                Text("Proceed to Payment")
            }
        }
    }
} 