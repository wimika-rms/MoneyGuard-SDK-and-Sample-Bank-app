package ng.wimika.samplebankapp.ui.screens.Login


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics

// For setting the testTag property inside the semantics block
import androidx.compose.ui.semantics.testTag

// For setting the contentDescription property (the other method we discussed)
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import ng.wimika.moneyguard_sdk.services.in_app_content.models.InAppContentResponse
import ng.wimika.samplebankapp.R
import ng.wimika.samplebankapp.ui.theme.SabiBankColors
import ng.wimika.samplebankapp.Constants
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.MoneyGuardClientApp.Companion.preferenceManager
import ng.wimika.samplebankapp.ui.screens.BottomSheetModal

private const val LOGIN_USERNAME_INPUT_ID = 1003 // Unique ID for the username EditText
private const val LOGIN_LOG_TAG = "MONEYGUARD_LOGGER"

// A reusable custom TextField composable to match the design
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    testTag: String = ""
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                // Apply the passed-in testTag inside the semantics block
                if (testTag.isNotEmpty()) {
                    this.testTag = testTag
                }
            },
        placeholder = { Text(placeholder, color = SabiBankColors.TextOnOrange.copy(alpha = 0.7f)) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SabiBankColors.OrangeDark,
            unfocusedContainerColor = SabiBankColors.OrangeDark,
            disabledContainerColor = SabiBankColors.OrangeDark,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = SabiBankColors.TextOnOrange
        ),
        textStyle = TextStyle(color = SabiBankColors.TextOnOrange, fontSize = 16.sp),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon
    )
}

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToVerification: () -> Unit,
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel { LoginViewModel() }
) {
    val context = LocalContext.current
    val sdkService = MoneyGuardClientApp.sdkService
    //val typingProfileService = remember { sdkService?.getTypingProfile() }
    
    val uiState by viewModel.uiState.collectAsState()
    var showRiskModal by remember { mutableStateOf(false) }
    var riskModalMessage by remember { mutableStateOf("") }
    var showCredentialDialog by remember { mutableStateOf(false) }
    var credentialDialogMessage by remember { mutableStateOf("") }
    var showUnusualLocationDialog by remember { mutableStateOf(false) }
    var showUntrustedDeviceDialog by remember { mutableStateOf(false) }
    var showTypingVerificationFailedDialog by remember { mutableStateOf(false) }
    var typingVerificationFailedMessage by remember { mutableStateOf("") }
    
    var usernameEditText by remember { mutableStateOf<EditText?>(null) }
    var permissionCheckTrigger by remember { mutableStateOf(0) }

    // --- Overlay Permission Handling ---
//    val settingsLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.StartActivityForResult()
//    ) {
//        Log.d(LOGIN_LOG_TAG, "[SampleBankApp|LoginScreen] Returned from overlay settings screen.")
//        permissionCheckTrigger++
//    }

    // Request overlay permission at startup
    LaunchedEffect(usernameEditText, permissionCheckTrigger) {
        if (usernameEditText == null) return@LaunchedEffect
        
//        if (Settings.canDrawOverlays(context)) {
//            try {
//                //typingProfileService?.startService(context as Activity, intArrayOf(LOGIN_USERNAME_INPUT_ID))
//                Log.d(LOGIN_LOG_TAG, "[SampleBankApp|LoginScreen] Overlay permission granted. Typing service started.")
//            } catch (e: Exception) {
//                Log.e(LOGIN_LOG_TAG, "[SampleBankApp|LoginScreen] Failed to start typing profile service", e)
//            }
//        } else {
//            Log.d(LOGIN_LOG_TAG, "[SampleBankApp|LoginScreen] Overlay permission not granted. Requesting user to enable.")
//            Toast.makeText(context, "Overlay permission is required for MoneyGuard features.", Toast.LENGTH_LONG).show()
//            val intent = Intent(
//                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                Uri.parse("package:${context.packageName}")
//            )
//            settingsLauncher.launch(intent)
//        }
    }

    // --- Side Effect Handling ---
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToDashboard -> onNavigateToDashboard()
                is LoginSideEffect.NavigateToVerification -> onNavigateToVerification()
                is LoginSideEffect.ShowRiskDialog -> {
                    riskModalMessage = LoginViewModel.getRiskMessage(effect.risk)
                    showRiskModal = true
                }
                is LoginSideEffect.HideRiskDialog -> showRiskModal = false
                is LoginSideEffect.ShowCredentialDialog -> {
                    credentialDialogMessage = effect.status
                    if(effect.status == "Credential Check - Could not determine status")
                    {

                    }
                    else{
                        showCredentialDialog = true
                    }
                }
                is LoginSideEffect.ShowUnusualLocationDialog -> showUnusualLocationDialog = true
                is LoginSideEffect.ShowUntrustedDeviceDialog -> showUntrustedDeviceDialog = true
                is LoginSideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
                is LoginSideEffect.ShowTypingVerificationFailedDialog -> {
                    typingVerificationFailedMessage = effect.message
                    showTypingVerificationFailedDialog = true
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SabiBankColors.White)
            ) {
                LoginHeader(modifier = Modifier.weight(0.35f))
                LoginForm(
                    modifier = Modifier.weight(0.65f),
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onUsernameEditTextCreated = { editText -> usernameEditText = editText }
                )
            }

            // --- Dialogs and Modals ---
//            if (uiState.isPrelaunchChecking) {
//                SecurityCheckOverlay()
//            }

            if (showRiskModal) {
                RiskBottomSheet(
                    message = riskModalMessage,
                    onDismiss = { viewModel.onEvent(LoginEvent.OnDismissRiskModal) }
                )
            }

            if (showCredentialDialog) {
                CredentialCheckDialog(
                    status = credentialDialogMessage,
                    onDismiss = {
                        showCredentialDialog = false
                        viewModel.onEvent(LoginEvent.OnDismissCredentialDialog)
                    }
                )
            }

            if (showUnusualLocationDialog) {
                UnusualLocationDialog(
                    onVerify = {
                        showUnusualLocationDialog = false
                        viewModel.onEvent(LoginEvent.OnDismissUnusualLocationDialogAndVerify)
                    },
                    onProceed = {
                        showUnusualLocationDialog = false
                        viewModel.onEvent(LoginEvent.OnDismissUnusualLocationDialogAndProceed)
                    }
                )
            }

            if (showUntrustedDeviceDialog) {
                UntrustedDeviceDialog(
                    onProceedToVerification = {
                        showUntrustedDeviceDialog = false
                        viewModel.onEvent(LoginEvent.OnDismissUntrustedDeviceDialog)
                    }
                )
            }

            if (showTypingVerificationFailedDialog) {
                TypingVerificationFailedDialog(
                    message = typingVerificationFailedMessage,
                    onProceedAnyway = {
                        showTypingVerificationFailedDialog = false
                        viewModel.onEvent(LoginEvent.OnDismissTypingVerificationFailedDialog)
                    }
                )
            }
        }
    }
}

// --- Extracted UI Components ---

@Composable
private fun LoginHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_graphic),
            contentDescription = "Sabi Bank Logo",
            modifier = Modifier.size(180.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "v${Constants.APP_VERSION}",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LoginForm(
    modifier: Modifier = Modifier,
    uiState: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onUsernameEditTextCreated: (EditText) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SabiBankColors.OrangePrimary,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter your login credentials",
            color = SabiBankColors.TextOnOrange,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Username field with typing pattern capture
        AndroidView(
            factory = { ctx ->
                EditText(ctx).apply {
                    id = LOGIN_USERNAME_INPUT_ID
                    hint = "Username"
                    contentDescription = "login_username_input"
                    setHintTextColor(SabiBankColors.TextOnOrange.copy(alpha = 0.7f).toArgb())
                    setTextColor(SabiBankColors.TextOnOrange.toArgb())
                    setBackgroundColor(SabiBankColors.OrangeDark.toArgb())
                    setPadding(40, 40, 40, 40)
                    inputType = InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
                    setSingleLine(true)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            onEvent(LoginEvent.OnUsernameChange(s?.toString() ?: ""))
                        }
                    })
                    onUsernameEditTextCreated(this)
                }
            },
            update = { editText ->
                if (editText.text.toString() != uiState.username) {
                    editText.setText(uiState.username)
                    editText.setSelection(uiState.username.length)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("login_username_input")
        )
        Spacer(modifier = Modifier.height(16.dp))

        SabiTextField(
            value = uiState.password,
            onValueChange = { onEvent(LoginEvent.OnPasswordChange(it)) },
            placeholder = "Password",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { onEvent(LoginEvent.OnTogglePasswordVisibility) },
                    modifier = Modifier.testTag("login_password_visibility_toggle")
                ) {
                    Icon(
                        imageVector = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (uiState.isPasswordVisible) "Hide password" else "Show password",
                        tint = SabiBankColors.TextOnOrange
                    )
                }
            },
            testTag = "login_password_input"
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = SabiBankColors.TextOnOrange,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(Color.Red.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        DebugLogToggle(
            isChecked = uiState.isDebugLogsEnabled,
            onCheckedChange = { onEvent(LoginEvent.OnDebugLogsToggle(it)) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onEvent(LoginEvent.OnLoginClick) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                testTag = "login_submit_button"
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = SabiBankColors.White,
                contentColor = SabiBankColors.OrangePrimary,
                disabledContainerColor = SabiBankColors.White.copy(alpha = 0.5f)
            ),
            enabled = !uiState.isLoading && uiState.username.isNotBlank() && uiState.password.isNotBlank()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = SabiBankColors.OrangePrimary,
                    strokeWidth = 3.dp
                )
            } else {
                Text("Login", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DebugLogToggle(isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Enable debug logs",
            color = SabiBankColors.TextOnOrange,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("login_debug_logs_switch"),
            colors = SwitchDefaults.colors(
                checkedThumbColor = SabiBankColors.White,
                checkedTrackColor = SabiBankColors.White.copy(alpha = 0.7f),
                uncheckedThumbColor = SabiBankColors.TextOnOrange.copy(alpha = 0.7f),
                uncheckedTrackColor = SabiBankColors.TextOnOrange.copy(alpha = 0.3f)
            )
        )
    }
}

// --- Overlays and Dialogs ---

@Composable
private fun SecurityCheckOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SabiBankColors.OrangePrimary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Performing security checks...", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun RiskBottomSheet(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        BottomSheetModal(
            title = "Pre-Launch Checks",
            message = message,
            buttonText = "Continue",
            onButtonClick = onDismiss
        )
    }
}

@Composable
private fun CredentialCheckDialog(status: String, onDismiss: () -> Unit) {
    val token = preferenceManager?.getMoneyGuardToken()
    val sdkService: ng.wimika.moneyguard_sdk.services.MoneyGuardSdkService? = MoneyGuardClientApp.sdkService
    
    var inAppContentResponse by remember { mutableStateOf<InAppContentResponse?>(null) }

    LaunchedEffect(Unit) {
        token?.let {
            val result = sdkService?.inAppContent()?.getInAppContent(it, 1)
            result?.onSuccess { response ->
                inAppContentResponse = response
            }
        }
    }

    // Use Elvis operator for default value and takeIf for cleaner null/empty check
    val dialogTitle = inAppContentResponse?.compromisedCredentialDialog?.title
        .takeIf { !it.isNullOrEmpty() } ?: "Credential Check"

    // Get dialog message from SDK if status is UNSAFE, otherwise use status
    val dialogMsg = if (status == "RISK_STATUS_UNSAFE") {
        inAppContentResponse?.compromisedCredentialDialog?.body ?: status
    } else {
        status
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(dialogTitle) },
        text = { Text(dialogMsg) },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
private fun UnusualLocationDialog(onVerify: () -> Unit, onProceed: () -> Unit) {
    val token = preferenceManager?.getMoneyGuardToken()
    val sdkService: ng.wimika.moneyguard_sdk.services.MoneyGuardSdkService? = MoneyGuardClientApp.sdkService
    
    var inAppContentResponse by remember { mutableStateOf<InAppContentResponse?>(null) }

    LaunchedEffect(Unit) {
        token?.let {
            val result = sdkService?.inAppContent()?.getInAppContent(it, 1)
            result?.onSuccess { response ->
                inAppContentResponse = response
            }
        }
    }

    // Use in-app content if available, otherwise use default hardcoded values
    val dialogTitle = inAppContentResponse?.unusualLocationDialog?.title
        .takeIf { !it.isNullOrEmpty() } ?: "Unusual Location Detected"
    
    val dialogBody = inAppContentResponse?.unusualLocationDialog?.body
        .takeIf { !it.isNullOrEmpty() } 
        ?: "We've detected a login from an unusual location. For your security, please verify your identity. If you proceed without verification, some account activities may be limited."

    AlertDialog(
        onDismissRequest = { /* Prevent dismissing */ },
        title = { Text(dialogTitle) },
        text = { Text(dialogBody) },
        confirmButton = {
            Button(onClick = onVerify) { Text("Verify") }
        },
        dismissButton = {
            TextButton(onClick = onProceed) { Text("Proceed without Verify") }
        }
    )
}

@Composable
private fun UntrustedDeviceDialog(onProceedToVerification: () -> Unit) {
    val token = preferenceManager?.getMoneyGuardToken()
    val sdkService: ng.wimika.moneyguard_sdk.services.MoneyGuardSdkService? = MoneyGuardClientApp.sdkService
    
    var inAppContentResponse by remember { mutableStateOf<InAppContentResponse?>(null) }

    LaunchedEffect(Unit) {
        token?.let {
            val result = sdkService?.inAppContent()?.getInAppContent(it, 1)
            result?.onSuccess { response ->
                inAppContentResponse = response
            }
        }
    }

    // Use in-app content if available, otherwise use default hardcoded values
    val dialogTitle = inAppContentResponse?.trustedDeviceDialog?.title
        .takeIf { !it.isNullOrEmpty() } ?: "Device Verification Required"
    
    val dialogBody = inAppContentResponse?.trustedDeviceDialog?.body
        .takeIf { !it.isNullOrEmpty() } 
        ?: "You are logging in from a different device than where MoneyGuard was initially installed.\n\nFor your security, we need to verify your identity before we can enable Moneyguard protection on this device."

    AlertDialog(
        onDismissRequest = { /* Prevent dismissing */ },
        title = {
            Text(
                text = dialogTitle,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = dialogBody,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onProceedToVerification,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SabiBankColors.OrangePrimary
                )
            ) {
                Text("Proceed to Verification")
            }
        }
    )
}

@Composable
private fun TypingVerificationFailedDialog(message: String, onProceedAnyway: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissing */ },
        title = {
            Text(
                text = "Typing Pattern Verification Failed",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
        },
        text = {
            Column {
                Text(
                    text = "Your typing pattern could not be verified. This may indicate unauthorized access.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
//                Text(
//                    text = "Reason: $message",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = Color.Gray
//                )
            }
        },
        confirmButton = {
            Button(
                onClick = onProceedAnyway,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SabiBankColors.OrangePrimary
                )
            ) {
                Text("Proceed Anyway")
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onNavigateToDashboard = {},
        onNavigateToVerification = {}
    )
}
