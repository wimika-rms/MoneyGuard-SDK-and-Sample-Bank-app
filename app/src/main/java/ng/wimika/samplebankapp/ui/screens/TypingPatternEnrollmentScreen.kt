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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ng.wimika.samplebankapp.MoneyGuardClientApp

private const val LOG_TAG = "typing-pattern-enroll"
private const val TYPING_PROFILE_INPUT_ID = 1001 // Unique ID for the EditText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingPatternScreen(
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit
) {
    // SDK and state variables
    val context = LocalContext.current
    val preferenceManager = MoneyGuardClientApp.preferenceManager
    val sdkService = MoneyGuardClientApp.sdkService
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3
    var userInput by remember { mutableStateOf("") }
    var editText by remember { mutableStateOf<EditText?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessBanner by remember { mutableStateOf(false) }

    // This state acts as a trigger to re-run the permission check.
    var permissionCheckTrigger by remember { mutableStateOf(0) }

    // Get user's name and construct the text to type
    val firstName = preferenceManager?.getMoneyguardFirstName()?.takeIf { it.isNotBlank() } ?: "John"
    val lastName = preferenceManager?.getMoneyguardLastName()?.takeIf { it.isNotBlank() } ?: "Doe"
    val textToType by remember {
        mutableStateOf("hello, my name is $firstName $lastName")
    }

    // Launcher for the settings screen. When the user returns, we increment the trigger.
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d(LOG_TAG, "Returned from overlay settings screen.")
        // Incrementing this trigger will cause the LaunchedEffect to re-run.
        permissionCheckTrigger++
    }

    // This effect now runs when the EditText is created OR when the permission trigger changes.
    LaunchedEffect(editText, permissionCheckTrigger) {
        if (editText == null) return@LaunchedEffect

        if (Settings.canDrawOverlays(context)) {
            // PERMISSION GRANTED: Start the service
            try {
                val typingProfile = sdkService?.getTypingProfile()
                typingProfile?.startService(context as Activity, intArrayOf(TYPING_PROFILE_INPUT_ID))
                Log.d(LOG_TAG, "Overlay permission granted. Typing service started.")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to start typing profile service", e)
            }
        } else {
            // PERMISSION NOT GRANTED: Navigate to settings
            Log.d(LOG_TAG, "Overlay permission not granted. Requesting user to enable.")
            Toast.makeText(context, "Overlay permission is required for this feature.", Toast.LENGTH_LONG).show()

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            settingsLauncher.launch(intent)
        }
    }

    // This effect triggers when the final step is completed
    LaunchedEffect(showSuccessBanner) {
        if (showSuccessBanner) {
            delay(5000L) // Wait for 1 second as requested
            onRegistrationComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Please type the words shown below") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
                MultiStepProgressBar(currentStep, totalSteps, Modifier.padding(top = 16.dp, bottom = 32.dp))
                Text(textToType, fontSize = 22.sp, lineHeight = 30.sp, color = Color.DarkGray, modifier = Modifier.padding(bottom = 24.dp))

                AndroidView(
                    factory = { ctx ->
                        EditText(ctx).apply {
                            id = TYPING_PROFILE_INPUT_ID
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
                        scope.launch {
                            isLoading = true
                            try {
                                val token = preferenceManager?.getMoneyGuardToken()
                                if (sdkService == null || token.isNullOrEmpty()) {
                                    Log.e(LOG_TAG, "SDK service or token is not available.")
                                    Toast.makeText(context, "Error: SDK not initialized.", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val result = sdkService.getTypingProfile().matchTypingProfile(userInput, token)
                                Log.d(LOG_TAG, "Step $currentStep result: $result")
                                if (result.success) {
                                    if (currentStep < totalSteps) {
                                        currentStep++; userInput = ""; editText?.setText(""); sdkService.getTypingProfile().resetService()
                                    } else {
                                        sdkService.getTypingProfile().stopService()
                                        showSuccessBanner = true
                                    }
                                } else {
                                    Toast.makeText(context, "Match failed: ${result.message}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Log.e(LOG_TAG, "An error occurred during typing match", e)
                                Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = userInput.isNotBlank() && !isLoading && !showSuccessBanner,
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
        AnimatedVisibility(
            visible = showSuccessBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp)
        ) {
            SuccessBanner()
        }
    }
}

// MultiStepProgressBar and SuccessBanner composables remain the same...
@Composable
private fun MultiStepProgressBar(currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..totalSteps) {
            val color = if (i <= currentStep) Color(0xFF8854F6) else Color(0xFFE0E0E0)
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(color))
        }
    }
}

@Composable
private fun SuccessBanner() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF8854F6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, "Success", tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Typing Pattern registered", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Your Behavioural capture was successful", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            }
        }
    }
}