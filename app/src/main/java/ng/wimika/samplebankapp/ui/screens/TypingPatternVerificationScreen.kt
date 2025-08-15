package ng.wimika.samplebankapp.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.ui.screens.ConsoleDialog
import ng.wimika.samplebankapp.loginRepo.ShareLogsRepositoryImpl
import ng.wimika.samplebankapp.Constants
import android.os.Build

// Define a sealed class to manage the state of the result dialog
private sealed class VerificationDialogState {
    object Hidden : VerificationDialogState()
    data class Shown(val isSuccess: Boolean, val message: String) : VerificationDialogState()
}

private const val VERIFY_LOG_TAG = "MONEYGUARD_LOGGER"
private const val VERIFY_TYPING_INPUT_ID = 1002 // Use a different ID to avoid conflicts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingPatternVerificationScreen(
    onVerificationResult: (isSuccess: Boolean) -> Unit
) {
    // SDK and state variables
    val context = LocalContext.current
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val sdkService = MoneyGuardClientApp.sdkService
    val scope = rememberCoroutineScope()
    val typingProfileService = remember { sdkService?.getTypingProfile() }

    var userInput by remember { mutableStateOf("") }
    var editText by remember { mutableStateOf<EditText?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var dialogState by remember { mutableStateOf<VerificationDialogState>(VerificationDialogState.Hidden) }
    var showConsoleDialog by remember { mutableStateOf(false) }
    var debugLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSharingLogs by remember { mutableStateOf(false) }
    var hasSharedLogs by remember { mutableStateOf(false) }

    // Get user's name for the prompt text
    //val firstName = preferenceManager?.getMoneyguardFirstName()?.takeIf { it.isNotBlank() } ?: "John"
    //val lastName = preferenceManager?.getMoneyguardLastName()?.takeIf { it.isNotBlank() } ?: "Doe"
    val fullName = preferenceManager?.getBankUserFullName()?.takeIf { it.isNotBlank() } ?: "John Doe"

    val textToType by remember {
        //mutableStateOf("hello, my name is $firstName $lastName")
        mutableStateOf("hello, my name is $fullName")
    }

    // Function to add debug logs
    fun addDebugLog(message: String) {
        if (preferenceManager?.isDebugLogsEnabled() == true) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            debugLogs = debugLogs + "[$timestamp] $message"
        }
    }

    // Function to share logs
    fun shareLogs() {
        scope.launch {
            isSharingLogs = true
            try {
                val userEmail = preferenceManager?.getUserEmail() ?: "unknown@user.com"
                val androidVersion = Build.VERSION.RELEASE
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val logContent = debugLogs.joinToString("\n")
                val appVersion = Constants.APP_VERSION

                val shareLogsRepository = ShareLogsRepositoryImpl()
                shareLogsRepository.shareLogs(
                    userEmail = userEmail,
                    androidVersion = androidVersion,
                    deviceModel = deviceModel,
                    logContent = logContent,
                    appVersion = appVersion
                ).collect { isSuccess ->
                    if (isSuccess) {
                        addDebugLog("Logs shared successfully")
                    } else {
                        addDebugLog("Failed to share logs")
                    }
                }
            } catch (e: Exception) {
                addDebugLog("Exception while sharing logs: ${e.message}")
            } finally {
                isSharingLogs = false
                hasSharedLogs = true
            }
        }
    }

    // --- Permission Handling (Reused from enrollment) ---
    var permissionCheckTrigger by remember { mutableStateOf(0) }
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d(VERIFY_LOG_TAG, "Returned from overlay settings screen.")
        permissionCheckTrigger++
    }

    LaunchedEffect(editText, permissionCheckTrigger) {
        if (editText == null) return@LaunchedEffect
        if (Settings.canDrawOverlays(context)) {
            try {
                typingProfileService?.startService(context as Activity, intArrayOf(VERIFY_TYPING_INPUT_ID))
                Log.d(VERIFY_LOG_TAG, "Overlay permission granted. Typing service started.")
                addDebugLog("Overlay permission granted. Typing service started.")
            } catch (e: Exception) {
                Log.e(VERIFY_LOG_TAG, "Failed to start typing profile service", e)
                addDebugLog("Failed to start typing profile service: ${e.message}")
            }
        } else {
            Toast.makeText(context, "Overlay permission is required for this feature.", Toast.LENGTH_LONG).show()
            addDebugLog("Overlay permission not granted. Requesting user to enable.")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            settingsLauncher.launch(intent)
        }
    }

    // Main UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Please type the words shown below") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            addDebugLog("User cancelled verification, stopping service")
                            typingProfileService?.stopService()
                            onVerificationResult(false) // Report failure/cancellation
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(textToType, fontSize = 22.sp, lineHeight = 30.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))

            AndroidView(
                factory = { ctx ->
                    EditText(ctx).apply {
                        id = VERIFY_TYPING_INPUT_ID
                        hint = "Type here"
                        setHintTextColor(Color.Gray.toArgb())
                        setTextColor(Color.Black.toArgb())
                        setBackgroundColor(Color(0xFFF5F6FA).toArgb())
                        setPadding(40, 40, 40, 40)
                        inputType = InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) { userInput = s?.toString() ?: "" }
                        })
                        editText = this
                    }
                },
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp))
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (userInput.trim() != textToType.trim()) {
                        Toast.makeText(context, "Please type the text exactly as shown.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        addDebugLog("Starting typing pattern verification")
                        try {
                            val token = preferenceManager?.getMoneyGuardToken()
                            Log.d(VERIFY_LOG_TAG, "[SampleBankApp|TypingPatternVerificationScreen] Token: ${token}")
                            if (typingProfileService == null || token.isNullOrEmpty()) {

                                addDebugLog("Error: SDK not initialized.")
                                Toast.makeText(context, "Error: SDK not initialized.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            addDebugLog("Token available, proceeding with verification")
                            val result = typingProfileService.verifyTypingProfile(userInput, token)
                            Log.d(VERIFY_LOG_TAG, "Verification result: $result")
                            addDebugLog("Verification result: ${result}")

                            val isSuccess = result.success && result.matched
//                            if (isSuccess) {
//                               // addDebugLog("Verification successful, service stopped ${result}")
//                            } else {
//                                //addDebugLog("Verification failed: ${result.message}")
//                            }

                            typingProfileService.stopService()
                            dialogState = VerificationDialogState.Shown(isSuccess, result.message)

                        } catch (e: Exception) {
                            Log.e(VERIFY_LOG_TAG, "An error occurred during verification", e)
                            addDebugLog("Exception during verification: ${e.message}")
                            dialogState = VerificationDialogState.Shown(false, e.message ?: "An unknown error occurred")
                        } finally {
                            isLoading = false
                            // Show console dialog if debug logs are enabled
                            if (preferenceManager?.isDebugLogsEnabled() == true) {
                                showConsoleDialog = true
                            }
                        }
                    }
                },
                enabled = userInput.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8854F6),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Verify", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // --- Result Dialog ---
    if (dialogState is VerificationDialogState.Shown) {
        val state = dialogState as VerificationDialogState.Shown
        AlertDialog(
            onDismissRequest = { /* Prevent dismissing by clicking outside */ },
            title = { Text(if (state.isSuccess) "Verification Successful" else "Verification Failed") },
            text = { Text(if (state.isSuccess) "Your identity has been verified." else "Your typing pattern could not be verified.") },
            confirmButton = {
                if (state.isSuccess) {
                    Button(onClick = { 
                        addDebugLog("User proceeding after successful verification")
                        onVerificationResult(true) 
                    }) { // Report success
                        Text("Proceed")
                    }
                } else {
                    Button(onClick = {
                        scope.launch {
                            addDebugLog("User chose to retry verification")
                            typingProfileService?.resetService()
                            typingProfileService?.startService(context as Activity, intArrayOf(VERIFY_TYPING_INPUT_ID))
                            userInput = ""
                            editText?.setText("")
                            dialogState = VerificationDialogState.Hidden
                        }
                    }) {
                        Text("Retry")
                    }
                }
            },
            dismissButton = {
                if (!state.isSuccess) {
                    TextButton(onClick = {
                        scope.launch {
                            addDebugLog("User chose to close verification")
                            typingProfileService?.stopService()
                            onVerificationResult(false) // Report failure
                        }
                    }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Console Dialog
    if (showConsoleDialog) {
        ConsoleDialog(
            logs = debugLogs,
            onClose = { 
                showConsoleDialog = false
                hasSharedLogs = false // Reset shared state when dialog closes
            },
            onShareLogs = { shareLogs() },
            isSharing = isSharingLogs,
            isShared = hasSharedLogs
        )
    }
}