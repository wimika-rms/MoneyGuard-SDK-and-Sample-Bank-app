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
import androidx.compose.runtime.rememberCoroutineScope
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
import ng.wimika.samplebankapp.loginRepo.StepUpRepository
import kotlinx.coroutines.launch
import ng.wimika.samplebankapp.MoneyGuardClientApp.Companion.preferenceManager
import ng.wimika.moneyguard_sdk_commons.types.SpecificRisk
import ng.wimika.moneyguard_sdk.services.utility.MoneyGuardAppStatus

data class TransactionData(
    val sourceAccountNumber: String,
    val destinationAccountNumber: String,
    val destinationBank: String,
    val destinationBankCode: String,
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
    var destinationBankCode by remember { mutableStateOf("") }
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

    // Dummy OTP step-up (demo only — no OTP is actually sent; the accepted code is fixed)
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var otpChallengeReference by remember { mutableStateOf("") }
    var otpVerifying by remember { mutableStateOf(false) }
    val stepUpRepository = remember { StepUpRepository() }
    val coroutineScope = rememberCoroutineScope()
    
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

        val activeRisks = result.risks
            .filter { it.status != RiskStatus.RISK_STATUS_SAFE && it.status != RiskStatus.RISK_STATUS_UNKNOWN }
        val flaggedRisks = activeRisks
            .mapNotNull { it.statusSummary ?: it.name }
            .distinct()
            .joinToString(", ")
        val risksLine = if (flaggedRisks.isNotEmpty()) "\n\nDetected: $flaggedRisks" else ""

        // Clear-and-immediate-danger risks escalate on their own, regardless of the
        // score band. They include the SDK's local prelaunch findings, which the
        // server's score never sees — so they must be honoured even on an Allow verdict.
        val standaloneHighRiskNames = listOf(
            SpecificRisk.SPECIFIC_RISK_NETWORK_WIFI_ENCRYPTION_NAME,
            SpecificRisk.SPECIFIC_RISK_NETWORK_DNS_SPOOFING_NAME,
            SpecificRisk.SPECIFIC_RISK_NETWORK_MITM_NAME,
            SpecificRisk.SPECIFIC_RISK_USER_IDENTITY_COMPROMISE_NAME
        )
        val standaloneHighRisk = result.status == RiskStatus.RISK_STATUS_UNSAFE_CREDENTIALS ||
                activeRisks.any { risk -> standaloneHighRiskNames.any { risk.name.contains(it) } }

        // OTP step-up applies when the score sits in the high-risk band or a standalone
        // high risk is present; a plain medium-band warning proceeds without OTP.
        val requiresOtp = result.riskLevel == "High" || standaloneHighRisk

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

            result.screeningDecision?.requiredAction == "BankOtpStepUp" -> {
                otpChallengeReference = result.screeningDecision?.challengeReference.orEmpty()
                showAlert = true
                alertTitle = "Blacklisted Account Warning"
                alertMessage = "This destination account is on your bank's blacklist. Cancel this transfer unless you are certain it is legitimate. Continuing requires bank OTP verification."
                alertButtonText = "Cancel Transfer"
                showSecondaryButton = true
                alertSecondaryButtonText = "Proceed with OTP"
                alertConfirmAction = { showAlert = false }
                alertSecondaryAction = {
                    showAlert = false
                    otpInput = ""
                    otpError = null
                    showOtpDialog = true
                }
            }

            result.verdict == TransactionVerdict.WARN ||
                    standaloneHighRisk ||
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
                        "\n\nProceeding is NOT recommended." +
                        if (requiresOtp) "\n\nAn OTP will be required to complete this transfer." else ""
                alertButtonText = "Cancel Transfer"
                showSecondaryButton = true
                alertSecondaryButtonText = "Proceed Anyway"
                alertConfirmAction = { showAlert = false }
                alertSecondaryAction = {
                    showAlert = false
                    if (requiresOtp) {
                        otpInput = ""
                        otpError = null
                        showOtpDialog = true
                    } else {
                        showTransferSuccess()
                    }
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
            destinationBankCode = data.destinationBankCode,
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
            destinationBankCode = destinationBankCode,
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
                        destinationBanks.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank.name) },
                                onClick = {
                                    destinationBank = bank.name
                                    destinationBankCode = bank.code
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

            // The demo code is checked by the Sabi bank API. MoneyGuard receives only
            // the opaque bank proof, never the OTP itself.
            if (showOtpDialog) {
                AlertDialog(
                    onDismissRequest = { showOtpDialog = false },
                    title = { Text("OTP Verification Required") },
                    text = {
                        Column {
                            Text(
                                "Because of the elevated risk on this transfer, a one-time " +
                                        "password has been sent to your registered phone number. " +
                                        "Enter it below to complete the transfer.\n\n" +
                                        "(Demo build: use 123456)"
                            )
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { input ->
                                    if (input.length <= 6 && input.all { it.isDigit() }) {
                                        otpInput = input
                                        otpError = null
                                    }
                                },
                                label = { Text("6-digit OTP") },
                                isError = otpError != null,
                                supportingText = { otpError?.let { Text(it) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .testTag("otp_input")
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val bankSession = preferenceManager?.getBankSessionId().orEmpty()
                                val challenge = otpChallengeReference.ifBlank {
                                    java.util.UUID.randomUUID().toString()
                                }
                                otpVerifying = true
                                coroutineScope.launch {
                                    runCatching {
                                        stepUpRepository.verify(bankSession, challenge, "transfer", otpInput)
                                    }.onSuccess {
                                        showOtpDialog = false
                                        showTransferSuccess()
                                    }.onFailure {
                                        otpError = it.message ?: "Incorrect OTP. Please try again."
                                    }
                                    otpVerifying = false
                                }
                            },
                            enabled = otpInput.length == 6 && !otpVerifying,
                            modifier = Modifier.testTag("otp_verify_button")
                        ) {
                            Text("Verify")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOtpDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

// Demo-only OTP accepted by the dummy step-up dialog; a real integration would
// verify against the bank's OTP service.
private data class DestinationBank(val name: String, val code: String)

private val destinationBanks = listOf(
    DestinationBank("GTBank", "058"),
    DestinationBank("Wema Bank", "035"),
    DestinationBank("OPay", "999992"),
    DestinationBank("Zenith Bank", "057"),
    DestinationBank("First Bank", "011"),
    DestinationBank("UBA", "033"),
    DestinationBank("Access Bank", "044")
)
