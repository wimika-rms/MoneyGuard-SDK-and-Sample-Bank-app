package ng.wimika.moneyguardsdkclient.ui.features.moneyguard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionScreen(
    moneyGuardPolicy: MoneyGuardPolicy,
    token: String,
    onBack: () -> Unit,
    onContinue: (List<BankAccount>) -> Unit
) {
    var accounts by remember { mutableStateOf<List<BankAccount>>(emptyList()) }
    var selectedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        moneyGuardPolicy.getUserAccounts(token, partnerBankId = 101).fold(
            onSuccess = { response ->
                accounts = response.bankAccounts
                isLoading = false
            },
            onFailure = { exception ->
                error = exception.message
                isLoading = false
            }
        )
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select All")
                    Checkbox(
                        checked = selectedAccounts.size == accounts.size,
                        onCheckedChange = { checked ->
                            selectedAccounts = if (checked) {
                                accounts.map { it.id.toString() }.toSet()
                            } else {
                                emptySet()
                            }
                        }
                    )
                }

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

                Button(
                    onClick = { 
                        val selectedBankAccounts = accounts.filter { account -> 
                            selectedAccounts.contains(account.id.toString()) 
                        }
                        onContinue(selectedBankAccounts)
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