package ng.wimika.samplebankapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.local.MoneyGuardSetupPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val sdkService = MoneyGuardClientApp.sdkService
    val token = preferenceManager?.getMoneyGuardToken()
    
    var accounts by remember { mutableStateOf<List<BankAccount>>(emptyList()) }
    var selectedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load accounts when screen is first loaded
    LaunchedEffect(Unit) {
        if (sdkService != null && !token.isNullOrEmpty()) {
            try {
                val moneyGuardPolicy = sdkService.policy()
                val result = moneyGuardPolicy.getUserAccounts(token, partnerBankId = 101)
                result.fold(
                    onSuccess = { response ->
                        accounts = response.bankAccounts
                        isLoading = false
                    },
                    onFailure = { exception ->
                        error = exception.message
                        isLoading = false
                        Toast.makeText(
                            context,
                            "Failed to load accounts: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                error = e.message
                isLoading = false
                Toast.makeText(
                    context,
                    "Error loading accounts: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Please login to view accounts",
                Toast.LENGTH_SHORT
            ).show()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Accounts to Cover") },
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
                .padding(horizontal = 16.dp, vertical = 54.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // Select All section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select All")
                    Checkbox(
                        checked = selectedAccounts.size == accounts.size && accounts.isNotEmpty(),
                        onCheckedChange = { checked ->
                            selectedAccounts = if (checked) {
                                accounts.map { it.id.toString() }.toSet()
                            } else {
                                emptySet()
                            }
                        }
                    )
                }

                // Accounts list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedAccounts.contains(account.id.toString()),
                                onCheckedChange = { checked ->
                                    selectedAccounts = if (checked) {
                                        selectedAccounts + account.id.toString()
                                    } else {
                                        selectedAccounts - account.id.toString()
                                    }
                                }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = account.number,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${account.type}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Continue button
                Button(
                    onClick = { 
                        // Save selected accounts to preferences
                        val currentPreferences = preferenceManager?.getMoneyGuardSetupPreferences() 
                            ?: MoneyGuardSetupPreferences()
                        val updatedPreferences = currentPreferences.copy(
                            accountIds = selectedAccounts.toList()
                        )
                        preferenceManager?.saveMoneyGuardSetupPreferences(updatedPreferences)
                        onContinue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = selectedAccounts.isNotEmpty()
                ) {
                    Text("Continue")
                }
            }
        }
    }
} 