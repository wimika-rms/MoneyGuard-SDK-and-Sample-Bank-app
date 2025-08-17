package ng.wimika.samplebankapp.ui.screens.Login


import android.Manifest
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import androidx.lifecycle.viewmodel.compose.viewModel
import ng.wimika.samplebankapp.R
import ng.wimika.samplebankapp.ui.theme.SabiBankColors
import ng.wimika.samplebankapp.Constants
import ng.wimika.samplebankapp.ui.screens.BottomSheetModal

// A reusable custom TextField composable to match the design
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
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
    val uiState by viewModel.uiState.collectAsState()
    var showRiskModal by remember { mutableStateOf(false) }
    var riskModalMessage by remember { mutableStateOf("") }
    var showCredentialDialog by remember { mutableStateOf(false) }
    var credentialDialogMessage by remember { mutableStateOf("") }
    var showUnusualLocationDialog by remember { mutableStateOf(false) }
    var showUntrustedDeviceDialog by remember { mutableStateOf(false) }

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
                    onEvent = viewModel::onEvent
                )
            }

            // --- Dialogs and Modals ---
            if (uiState.isPrelaunchChecking) {
                SecurityCheckOverlay()
            }

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
    onEvent: (LoginEvent) -> Unit
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

        SabiTextField(
            value = uiState.username,
            onValueChange = { onEvent(LoginEvent.OnUsernameChange(it)) },
            placeholder = "Username",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(16.dp))

        SabiTextField(
            value = uiState.password,
            onValueChange = { onEvent(LoginEvent.OnPasswordChange(it)) },
            placeholder = "Password",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { onEvent(LoginEvent.OnTogglePasswordVisibility) }) {
                    Icon(
                        imageVector = if (uiState.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (uiState.isPasswordVisible) "Hide password" else "Show password",
                        tint = SabiBankColors.TextOnOrange
                    )
                }
            }
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
            modifier = Modifier.fillMaxWidth().height(56.dp),
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
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Credential Check") },
        text = { Text(status) },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
private fun UnusualLocationDialog(onVerify: () -> Unit, onProceed: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissing */ },
        title = { Text("Unusual Location Detected") },
        text = { Text("We've detected a login from an unusual location. For your security, please verify your identity. If you proceed without verification, some account activities may be limited.") },
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
    AlertDialog(
        onDismissRequest = { /* Prevent dismissing */ },
        title = { 
            Text(
                text = "Device Verification Required",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Column {
                Text(
                    text = "You are logging in from a different device than where MoneyGuard was initially installed.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "For your security, we need to verify your identity before we can enable Moneyguard protection on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onNavigateToDashboard = {},
        onNavigateToVerification = {}
    )
}
