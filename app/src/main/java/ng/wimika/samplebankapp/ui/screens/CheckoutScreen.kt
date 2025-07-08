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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
    val flowState = MoneyGuardClientApp.accountProtectionFlowState
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }
    var modalTitle by remember { mutableStateOf("") }
    var modalMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    // Account selection state
    var expanded by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf(flowState?.selectedDebitAccount) }
    var accounts by remember { mutableStateOf(flowState?.allAccounts ?: emptyList()) }
    var accountsLoading by remember { mutableStateOf(flowState?.allAccounts?.isEmpty() != false) }
    var accountsError by remember { mutableStateOf<String?>(null) }

    // Fetch accounts from SDK (only if not already loaded)
    LaunchedEffect(Unit) {
        if (flowState?.allAccounts?.isEmpty() != false && sdkService != null && !token.isNullOrEmpty()) {
            try {
                val moneyGuardPolicy = sdkService.policy()
                val result = moneyGuardPolicy.getUserAccounts(token, partnerBankId = 101)
                result.fold(
                    onSuccess = { response ->
                        accounts = response.bankAccounts
                        flowState?.setAllAccounts(response.bankAccounts)
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
        } else if (flowState?.allAccounts?.isEmpty() == false) {
            // Already have accounts loaded, just update local state
            accounts = flowState.allAccounts
            accountsLoading = false
        } else {
            accountsError = "Please login to view accounts"
            accountsLoading = false
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Checkout",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Summary box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(0xFF8854F6),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(Color(0xFFF8F6FF), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "The account chosen will be debited for subsequent subscription renewals.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xFF6B6B6B)
                        )
                        Text(
                            text = flowState?.getSubscriptionPlanDisplay() ?: "",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = Color(0xFF8854F6)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Payment Options
                Text(
                    text = "Payment Options",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(0xFF8854F6),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(Color.White, shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = true,
                                onClick = {},
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8854F6))
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "Direct Account Debit",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                                Text(
                                    text = "Pay directly from your bank account",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Account selection dropdown
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
                                                flowState?.setSelectedDebitAccount(account)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = "The account chosen will be debited for subsequent subscription renewals.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFF6B6B6B),
                            modifier = Modifier.padding(top = 8.dp)
                        )
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
                                    policyOptionId = flowState?.selectedPolicyOption?.id?.toString() ?: "",
                                    coveredAccountIds = flowState?.selectedAccountIds?.toList() ?: emptyList(),
                                    debitAccountId = selectedAccount?.id.toString(),
                                    autoRenew = flowState?.autoRenew ?: true
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8854F6)),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Pay", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 18.sp))
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