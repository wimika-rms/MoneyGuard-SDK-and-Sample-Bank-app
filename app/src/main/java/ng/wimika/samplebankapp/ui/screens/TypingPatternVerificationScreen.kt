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

// Define a sealed class to manage the state of the result dialog
private sealed class VerificationDialogState {
    object Hidden : VerificationDialogState()
    data class Shown(val isSuccess: Boolean, val message: String) : VerificationDialogState()
}

private const val VERIFY_LOG_TAG = "typing-pattern-verify"
private const val VERIFY_TYPING_INPUT_ID = 1002 // Use a different ID to avoid conflicts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingPatternVerificationScreen(
    onVerificationSuccess: () -> Unit,
    onClose: () -> Unit
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

    // Get user's name for the prompt text
    val firstName = preferenceManager?.getMoneyguardFirstName()?.takeIf { it.isNotBlank() } ?: "John"
    val lastName = preferenceManager?.getMoneyguardLastName()?.takeIf { it.isNotBlank() } ?: "Doe"
    val textToType by remember {
        mutableStateOf("hello, my name is $firstName $lastName")
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
            } catch (e: Exception) {
                Log.e(VERIFY_LOG_TAG, "Failed to start typing profile service", e)
            }
        } else {
            Toast.makeText(context, "Overlay permission is required for this feature.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            settingsLauncher.launch(intent)
        }
    }

    // Main UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Typing Pattern") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            typingProfileService?.stopService()
                            onClose()
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
            Text(textToType, fontSize = 22.sp, lineHeight = 30.sp, color = Color.DarkGray, modifier = Modifier.padding(bottom = 24.dp))

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
                        try {
                            val token = preferenceManager?.getMoneyGuardToken()
                            if (typingProfileService == null || token.isNullOrEmpty()) {
                                Toast.makeText(context, "Error: SDK not initialized.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val result = typingProfileService.matchTypingProfile(userInput, token)
                            Log.d(VERIFY_LOG_TAG, "Verification result: $result")

                            val isSuccess = result.success && result.matched
                            if (isSuccess) {
                                typingProfileService.stopService()
                            }
                            dialogState = VerificationDialogState.Shown(isSuccess, result.message)

                        } catch (e: Exception) {
                            Log.e(VERIFY_LOG_TAG, "An error occurred during verification", e)
                            dialogState = VerificationDialogState.Shown(false, e.message ?: "An unknown error occurred")
                        } finally {
                            isLoading = false
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
                    Text("Submit", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                    Button(onClick = onVerificationSuccess) {
                        Text("Proceed")
                    }
                } else {
                    Button(onClick = {
                        scope.launch {
                            typingProfileService?.resetService()
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
                            typingProfileService?.stopService()
                            onClose()
                        }
                    }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}