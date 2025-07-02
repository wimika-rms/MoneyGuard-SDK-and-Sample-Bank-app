package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.local.MoneyGuardSetupPreferences
import kotlinx.coroutines.launch
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val sdkService = MoneyGuardClientApp.sdkService
    val token = preferenceManager?.getMoneyGuardToken()
    val prefs = preferenceManager?.getMoneyGuardSetupPreferences() ?: MoneyGuardSetupPreferences()
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }
    var modalTitle by remember { mutableStateOf("") }
    var modalMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    // Account selection state
    var expanded by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<BankAccount?>(null) }
    var accounts by remember { mutableStateOf<List<BankAccount>>(emptyList()) }
    var accountsLoading by remember { mutableStateOf(true) }
    var accountsError by remember { mutableStateOf<String?>(null) }

    // Fetch accounts from SDK
    LaunchedEffect(Unit) {
        if (sdkService != null && !token.isNullOrEmpty()) {
            try {
                val moneyGuardPolicy = sdkService.policy()
                val result = moneyGuardPolicy.getUserAccounts(token, partnerBankId = 101)
                result.fold(
                    onSuccess = { response ->
                        accounts = response.bankAccounts
                        accountsLoading = false
                    },
                    onFailure = { exception ->
                        accountsError = exception.message
                        accountsLoading = false
                    }
                )
            } catch (e: Exception) {
                accountsError = e.message
                accountsLoading = false
            }
        } else {
            accountsError = "Please login to view accounts"
            accountsLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ready to complete your MoneyGuard purchase?", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(24.dp))
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
                                    .menuAnchor(),
                                enabled = !accountsLoading && accountsError == null
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                if (accountsLoading) {
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.padding(8.dp))
                                    }
                                } else if (accountsError != null) {
                                    Text(accountsError ?: "Error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
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
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val policy = sdkService?.policy()
                                val result = policy?.createPolicy(
                                    token = token ?: "",
                                    policyOptionId = prefs.policyOptionId,
                                    coveredAccountIds = prefs.accountIds,
                                    debitAccountId = selectedAccount?.id.toString(),
                                    autoRenew = prefs.autoRenew
                                )
                                result?.fold(
                                    onSuccess = {
                                        isSuccess = true
                                        modalTitle = "Congratulations"
                                        modalMessage = "Your transaction was successful!"
                                        showModal = true
                                    },
                                    onFailure = {
                                        isSuccess = false
                                        modalTitle = "Oops"
                                        modalMessage = "Something went wrong. Your transaction has been declined."
                                        showModal = true
                                    }
                                )
                            } catch (e: Exception) {
                                isSuccess = false
                                modalTitle = "Oops"
                                modalMessage = "Something went wrong. Your transaction has been declined."
                                showModal = true
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = selectedAccount != null && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Proceed", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (showModal) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                ) {
                    BottomSheetModal(
                        title = modalTitle,
                        message = modalMessage,
                        buttonText = "Proceed",
                        onButtonClick = {
                            showModal = false
                            if (isSuccess) onProceed() else onBack()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
} 