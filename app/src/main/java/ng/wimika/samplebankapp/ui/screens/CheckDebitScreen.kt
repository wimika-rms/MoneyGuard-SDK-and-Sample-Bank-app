package ng.wimika.samplebankapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import ng.wimika.moneyguard_sdk_commons.types.RiskStatus
import ng.wimika.moneyguard_sdk_commons.types.TransactionVerdict
import ng.wimika.moneyguard_sdk.services.transactioncheck.models.DebitTransaction
import ng.wimika.moneyguard_sdk.services.transactioncheck.models.DebitTransactionCheckResult
import ng.wimika.moneyguard_sdk.services.transactioncheck.models.LatLng
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.MoneyGuardClientApp.Companion.preferenceManager
import ng.wimika.moneyguard_sdk_commons.types.SpecificRisk
import ng.wimika.moneyguard_sdk.services.utility.MoneyGuardAppStatus

data class TransactionData(
    val sourceAccountNumber: String,
    val destinationAccountNumber: String,
    val destinationBank: String,
    val memo: String,
    val amount: Double,
    //val geoLocation: GeoLocation
)

data class GeoLocation(
    val lat: Double,
    val lon: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckDebitScreen(
    onLocationPermissionDismissed: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onDownloadMoneyGuard: () -> Unit = {}
) {
    val context = LocalContext.current
    
    var hasLocationPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var amount by remember { mutableStateOf("") }
    var sourceAccountNumber by remember { mutableStateOf("") }
    var sourceAccountExpanded by remember { mutableStateOf(false) }
    var destinationAccountNumber by remember { mutableStateOf("") }
    var destinationBank by remember { mutableStateOf("") }
    var destinationBankExpanded by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    
    var showAlert by remember { mutableStateOf(false) }
    var alertTitle by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("") }
    var alertButtonText by remember { mutableStateOf("OK") }
    var alertSecondaryButtonText by remember { mutableStateOf<String?>(null) }
    var showSecondaryButton by remember { mutableStateOf(false) }
    var alertConfirmAction by remember { mutableStateOf<() -> Unit>({}) }
    var alertSecondaryAction by remember { mutableStateOf<() -> Unit>({}) }
    
    // Add new state variables for policy status
    var moneyguardStatus by remember { mutableStateOf<MoneyGuardAppStatus?>(null) }
    var showPolicyAlert by remember { mutableStateOf(false) }
    
    val enableButton = amount.isNotEmpty() && sourceAccountNumber.isNotEmpty() &&
                      destinationAccountNumber.isNotEmpty() && destinationBank.isNotEmpty() && 
                      !isLoading

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        hasLocationPermissions = fineLocationGranted && coarseLocationGranted
        
        if (!hasLocationPermissions) {
            onLocationPermissionDismissed()
        }
    }

    // Add new LaunchedEffect to check policy status
    LaunchedEffect(Unit) {
        val token = preferenceManager?.getMoneyGuardToken() ?: ""
        moneyguardStatus = MoneyGuardClientApp.sdkService?.utility()?.checkMoneyguardPolicyStatus(token)
        
        if (moneyguardStatus == MoneyGuardAppStatus.ValidPolicyAppNotInstalled) {
            showPolicyAlert = true
        }
    }

    // Consolidated security checks in proper priority order.
    // (The old cached risk-score-vs-threshold gate was removed: the threshold decision
    // is now made per-transaction by the backend and arrives as the debit-check verdict.)
    LaunchedEffect(Unit) {
        // FIRST PRIORITY: Identity compromised check
        val identityCompromised = preferenceManager?.isIdentityCompromised() ?: false
        if (identityCompromised) {
            showAlert = true
            alertTitle = "Identity Compromised"
            alertMessage = "Your banking login credentials have been compromised, please update your password before you can proceed with your transaction."
            alertButtonText = "OK"
            showSecondaryButton = false
            alertConfirmAction = { 
                showAlert = false
                onBackClick() // Navigate back to dashboard
            }
            return@LaunchedEffect // Exit early to prevent other checks
        }

        // SECOND PRIORITY: Check for specific risks from risk register that prevent transactions
        val riskRegister = preferenceManager?.getRiskRegister() ?: emptyList()
        
        when {
            riskRegister.contains(SpecificRisk.SPECIFIC_RISK_APPLICATION_MALWARE_NAME) -> {
                showAlert = true
                alertTitle = "Malware Detected"
                alertMessage = "Malware has been detected on your device that could compromise your transaction. Please remove the malware before proceeding with any financial transactions."
                alertButtonText = "OK"
                showSecondaryButton = false
                alertConfirmAction = { 
                    showAlert = false
                    onBackClick() // Navigate back to dashboard
                }
            }
            riskRegister.contains(SpecificRisk.SPECIFIC_RISK_NETWORK_WIFI_ENCRYPTION_NAME) || 
            riskRegister.contains(SpecificRisk.SPECIFIC_RISK_NETWORK_WIFI_PASSWORD_PROTECTION_NAME) -> {
                showAlert = true
                alertTitle = "Unsecure Network"
                alertMessage = "You are connected to an unencrypted or unsecure WiFi network. Please disconnect and connect to a secure WiFi network before proceeding with your transaction."
                alertButtonText = "OK"
                showSecondaryButton = false
                alertConfirmAction = { 
                    showAlert = false
                    onBackClick() // Navigate back to dashboard
                }
            }
            riskRegister.contains(SpecificRisk.SPECIFIC_RISK_DEVICE_ROOT_OR_JAILBREAK_NAME) -> {
                showAlert = true
                alertTitle = "Device Security Compromised"
                alertMessage = "Your device has been rooted/jailbroken which compromises its security. Financial transactions cannot be performed on this device for your safety."
                alertButtonText = "OK"
                showSecondaryButton = false
                alertConfirmAction = { 
                    showAlert = false
                    onBackClick() // Navigate back to dashboard
                }
            }
            riskRegister.contains(SpecificRisk.SPECIFIC_RISK_NETWORK_MITM_NAME) -> {
                showAlert = true
                alertTitle = "Network Security Risk"
                alertMessage = "A man-in-the-middle attack has been detected on your network connection. Please change to a secure network before proceeding with your transaction."
                alertButtonText = "OK"
                showSecondaryButton = false
                alertConfirmAction = { 
                    showAlert = false
                    onBackClick() // Navigate back to dashboard
                }
            }
        }
    }

//    LaunchedEffect(hasLocationPermissions) {
//        if (hasLocationPermissions) {
//            try {
//                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
//                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
//                    .addOnSuccessListener { location: Location? ->
//                        location?.let {
//                            latitude = it.latitude
//                            longitude = it.longitude
//                        }
//                    }
//            } catch (e: SecurityException) {
//                // Handle security exception
//                showAlert = true
//                alertTitle = "Location Error"
//                alertMessage = "Unable to get current location: ${e.message}"
//                alertButtonText = "OK"
//                showSecondaryButton = false
//                alertConfirmAction = { showAlert = false }
//            }
//        }
//    }

    // Check permissions when screen loads
//    LaunchedEffect(Unit) {
//        if (!hasLocationPermissions) {
//            locationPermissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                )
//            )
//        }
//    }

    fun showTransferSuccess() {
        isLoading = false
        showAlert = true
        alertTitle = "Transfer Successful ✅"
        alertMessage = "Your transfer has been completed successfully."
        alertButtonText = "OK"
        showSecondaryButton = false
        alertConfirmAction = { showAlert = false }
    }

    fun handleRiskStatus(result: DebitTransactionCheckResult) {
        isLoading = false

        val scoreLine = result.riskScorePercent?.let { percent ->
            "\n\nSafety score: ${percent.toInt()}% — ${result.riskLevel ?: "Unclassified"} risk."
        } ?: ""

        val flaggedRisks = result.risks
            .filter { it.status != RiskStatus.RISK_STATUS_SAFE && it.status != RiskStatus.RISK_STATUS_UNKNOWN }
            .mapNotNull { it.statusSummary ?: it.name }
            .distinct()
            .joinToString(", ")
        val risksLine = if (flaggedRisks.isNotEmpty()) "\n\nDetected: $flaggedRisks" else ""

        // The verdict is the server's decision against this bank's configured risk
        // thresholds. Block removes the option to continue; Warn continues only through
        // an explicit, non-recommended override. When the verdict is absent (older
        // backend or failed call) the advisory status rollup decides.
        when {
            result.verdict == TransactionVerdict.BLOCK -> {
                showAlert = true
                alertTitle = "Transaction Blocked"
                alertMessage = "This transaction was blocked because the current risk on this " +
                        "device or session is above your bank's allowed level." +
                        risksLine + scoreLine
                alertButtonText = "Back to Dashboard"
                showSecondaryButton = false
                alertConfirmAction = {
                    showAlert = false
                    onBackClick()
                }
            }

            result.verdict == TransactionVerdict.WARN ||
                    (result.verdict == null && result.status != RiskStatus.RISK_STATUS_SAFE &&
                            result.status != RiskStatus.RISK_STATUS_UNKNOWN) -> {
                val title = when (result.status) {
                    RiskStatus.RISK_STATUS_UNSAFE_CREDENTIALS -> "Compromised Credentials"
                    RiskStatus.RISK_STATUS_UNSAFE_LOCATION -> "Suspicious Location"
                    else -> "High Risk Warning"
                }
                showAlert = true
                alertTitle = title
                alertMessage = "We have detected threats that put this transaction at risk." +
                        risksLine + scoreLine +
                        "\n\nProceeding is NOT recommended."
                alertButtonText = "Cancel Transfer"
                showSecondaryButton = true
                alertSecondaryButtonText = "Proceed Anyway"
                alertConfirmAction = { showAlert = false }
                alertSecondaryAction = {
                    showAlert = false
                    showTransferSuccess()
                }
            }

            else -> showTransferSuccess()
        }
    }

    fun checkDebitTransaction(data: TransactionData) {
        isLoading = true
        val preferenceManager = MoneyGuardClientApp.preferenceManager
        val sessionToken = preferenceManager?.getMoneyGuardToken() ?: ""
        val transactionCheck = MoneyGuardClientApp.sdkService?.transactionCheck()
        
        val debitTransaction = DebitTransaction(
            sourceAccountNumber = data.sourceAccountNumber,
            destinationAccountNumber = data.destinationAccountNumber,
            destinationBank = data.destinationBank,
            memo = data.memo,
            amount = data.amount
        )

        transactionCheck?.checkDebitTransaction(sessionToken, debitTransaction,
            onSuccess = { result ->
                if (result.success) {
                    handleRiskStatus(result)
                } else {
                    isLoading = false
                    showAlert = true
                    alertTitle = "Transaction Failed"
                    alertMessage = "Transaction check failed. Please try again."
                    alertButtonText = "OK"
                    showSecondaryButton = false
                    alertConfirmAction = { showAlert = false }
                }
            },
            onFailure = {
                isLoading = false
                showAlert = true
                alertTitle = "Error"
                alertMessage = "Failed to check transaction. Please try again."
                alertButtonText = "OK"
                showSecondaryButton = false
                alertConfirmAction = { showAlert = false }
            }
        )
    }

    fun handleCheckDebitClick() {
//        if (!hasLocationPermissions) {
//            showAlert = true
//            alertTitle = "Location Required"
//            alertMessage = "Location permissions are required for this transaction."
//            alertButtonText = "OK"
//            showSecondaryButton = false
//            alertConfirmAction = { showAlert = false }
//            return
//        }
        
        val amountDouble = amount.toDoubleOrNull()
        if (amountDouble == null) {
            showAlert = true
            alertTitle = "Invalid Amount"
            alertMessage = "Please enter a valid amount."
            alertButtonText = "OK"
            showSecondaryButton = false
            alertConfirmAction = { showAlert = false }
            return
        }
        
        val transactionData = TransactionData(
            sourceAccountNumber = sourceAccountNumber,
            destinationAccountNumber = destinationAccountNumber,
            destinationBank = destinationBank,
            memo = memo,
            amount = amountDouble,
        )
        
        checkDebitTransaction(transactionData)
    }

    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Make Transfer",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("check_debit_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }, containerColor = Color.White) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("check_debit_amount_input"),
                    value = amount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    onValueChange = { newAmount ->
                        // Validate that it's a valid number
                        if (newAmount.isEmpty() || newAmount.toDoubleOrNull() != null) {
                            amount = newAmount
                        }
                    },
                    label = { Text("Amount") },
                )

                ExposedDropdownMenuBox(
                    expanded = sourceAccountExpanded,
                    onExpandedChange = { sourceAccountExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .menuAnchor()
                            .testTag("check_debit_source_account_input"),
                        readOnly = true,
                        value = sourceAccountNumber,
                        onValueChange = { },
                        label = { Text("Source Account Number") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceAccountExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = sourceAccountExpanded,
                        onDismissRequest = { sourceAccountExpanded = false }
                    ) {
                        listOf("0123456789", "9876543210").forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account) },
                                onClick = {
                                    sourceAccountNumber = account
                                    sourceAccountExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = destinationAccountNumber,
                    onValueChange = { destinationAccountNumber = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(text = "Destination Account Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("check_debit_destination_account_input")
                )

                ExposedDropdownMenuBox(
                    expanded = destinationBankExpanded,
                    onExpandedChange = { destinationBankExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .menuAnchor()
                            .testTag("check_debit_destination_bank_input"),
                        readOnly = true,
                        value = destinationBank,
                        onValueChange = { },
                        label = { Text("Destination Bank") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destinationBankExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = destinationBankExpanded,
                        onDismissRequest = { destinationBankExpanded = false }
                    ) {
                        listOf("GTB", "Wema", "Opay", "Zenith Bank", "First Bank", "UBA", "Access Bank").forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank) },
                                onClick = {
                                    destinationBank = bank
                                    destinationBankExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Memo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("check_debit_memo_input"),
                    minLines = 3
                )

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF97316),
                        contentColor = Color.White
                    ),
                    enabled = enableButton,
                    onClick = { handleCheckDebitClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("check_debit_submit_button")
                        //.height(56.dp)

                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFFF97316)
                        )
                    } else {
                        Text("Transfer")
                    }
                }

                Box(modifier = Modifier.padding(top = 16.dp))

//                Text("Current Location")
//                Text(
//                    text = if (hasLocationPermissions) {
//                        "Longitude: $longitude, Latitude: $latitude"
//                    } else {
//                        "Location permissions not granted"
//                    }
//                )
            }

            // Add new alert dialog for policy status (show first)
            if (showPolicyAlert) {
                AlertDialog(
                    onDismissRequest = { showPolicyAlert = false },
                    title = { Text("Protect your account") },
                    text = { 
                        Text("For your security, we recommend installing MoneyGuard before proceeding with transactions.") 
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showPolicyAlert = false
                                onDownloadMoneyGuard() // Navigate to download screen
                            }
                        ) {
                            Text("Download MoneyGuard")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showPolicyAlert = false 
                                // User chooses to proceed anyway
                            }
                        ) {
                            Text("Proceed anyway")
                        }
                    }
                )
            }

            if (showAlert) {
                AlertDialog(
                    onDismissRequest = { showAlert = false },
                    title = { Text(alertTitle) },
                    text = { Text(alertMessage) },
                    confirmButton = {
                        TextButton(
                            onClick = alertConfirmAction
                        ) {
                            Text(alertButtonText)
                        }
                    },
                    dismissButton = if (showSecondaryButton && alertSecondaryButtonText != null) {
                        {
                            TextButton(
                                onClick = alertSecondaryAction
                            ) {
                                Text(alertSecondaryButtonText!!)
                            }
                        }
                    } else null
                )
            }
        }
    }
}