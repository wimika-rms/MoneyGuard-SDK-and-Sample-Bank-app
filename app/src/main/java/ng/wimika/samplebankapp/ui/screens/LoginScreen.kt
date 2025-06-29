package ng.wimika.samplebankapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.launch
import ng.wimika.samplebankapp.R // Make sure this import points to your project's R file
import ng.wimika.samplebankapp.loginRepo.LoginRepositoryImpl
import ng.wimika.moneyguard_sdk_commons.types.MoneyGuardResult
import ng.wimika.samplebankapp.MoneyGuardClientApp // Assuming these imports are correct
import ng.wimika.samplebankapp.local.IPreferenceManager
import ng.wimika.samplebankapp.Constants

// --- New UI Code Starts Here ---

// Define colors from the new design for easy reuse
private object SabiBankColors {
    val OrangePrimary = Color(0xFFD95F29)
    val OrangeDark = Color(0xFFC05425) // A darker shade for text fields
    val White = Color.White
    val TextPrimaryOrange = Color(0xFFD95F29)
    val TextOnOrange = Color.White
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    // --- All existing state and logic is preserved ---
    var username by remember { mutableStateOf("") } // Pre-filled from screenshot
    var password by remember { mutableStateOf("********") } // Pre-filled from screenshot
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    // In a real app, you'd use dependency injection for the repository
    val loginRepository = remember { LoginRepositoryImpl() }

    // --- The UI is structured into two main parts ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SabiBankColors.White)
    ) {
        // --- Top White Section (Logo) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f), // Takes ~35% of the screen height
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_graphic),
                contentDescription = "Sabi Bank Logo",
                modifier = Modifier.size(180.dp)
            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Image(
//                painter = painterResource(id = R.drawable.logo_text),
//                contentDescription = "Sabi Bank",
//                modifier = Modifier.height(30.dp)
//            )
        }

        // --- Bottom Orange Section (Login Form) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f) // Takes ~65% of the screen height
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

            // Username Field
            SabiTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            SabiTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = PasswordVisualTransformation()
            )

            // Forgot Password Link
//            TextButton(
//                onClick = { /* TODO: Handle Forgot Password */ },
//                modifier = Modifier.align(Alignment.End)
//            ) {
//                Text(
//                    text = "Forgot Password",
//                    color = SabiBankColors.TextOnOrange,
//                    fontSize = 14.sp
//                )
//            }

            if (showError) {
                Text(
                    text = "Login failed. Please check your credentials.",
                    color = SabiBankColors.TextOnOrange,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .background(Color.Red.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }


            Spacer(modifier = Modifier.weight(1f)) // Pushes content to top and bottom

            // Login Button
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        scope.launch {
                            isLoading = true
                            showError = false

                            try {
                                loginRepository.login(username, password).collect { response ->
                                    val sessionData = response.data
                                    if (sessionData != null && sessionData.sessionId.isNotEmpty()) {
                                        val preferenceManager = MoneyGuardClientApp.preferenceManager
                                        preferenceManager?.saveBankLoginDetails(
                                            sessionData.sessionId,
                                            sessionData.userFullName
                                        )
                                        registerWithMoneyguard(sessionData.sessionId, preferenceManager, onLoginSuccess)
                                    } else {
                                        showError = true
                                    }
                                }
                            } catch (e: Exception) {
                                showError = true
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50), // Pill shape
                colors = ButtonDefaults.buttonColors(
                    containerColor = SabiBankColors.White,
                    contentColor = SabiBankColors.TextPrimaryOrange,
                    disabledContainerColor = SabiBankColors.White.copy(alpha = 0.5f)
                ),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = SabiBankColors.TextPrimaryOrange,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text("Login", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Open an account Link
//            TextButton(onClick = { /* TODO: Handle Open Account */ }) {
//                Text(
//                    "Open an account",
//                    color = SabiBankColors.TextOnOrange,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
        }
    }
}

// A reusable custom TextField composable to match the design
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SabiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = SabiBankColors.TextOnOrange.copy(alpha = 0.7f)) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors( // <-- CORRECTED: Was textFieldColors, now it's just .colors
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
        visualTransformation = visualTransformation
    )
}

// --- The existing helper function is preserved exactly as it was ---
private suspend fun registerWithMoneyguard(
    sessionId: String,
    preferenceManager: IPreferenceManager?,
    onLoginSuccess: () -> Unit
) {
    try {
        val sdkService = MoneyGuardClientApp.sdkService
        sdkService?.authentication()?.register(
            parteBankId = Constants.PARTNER_BANK_ID,
            partnerSessionToken = sessionId
        )?.collect { result ->
            when (result) {
                is MoneyGuardResult.Success -> {
                    val sessionResponse = result.data
                    if (sessionResponse.token.isNotEmpty()) {
                        preferenceManager?.saveMoneyguardEnabled(true)
                        preferenceManager?.saveMoneyGuardToken(sessionResponse.token)
                        preferenceManager?.saveMoneyguardUserNames(
                            sessionResponse.userDetails.firstName,
                            sessionResponse.userDetails.lastName
                        )
                    } else {
                        preferenceManager?.saveMoneyguardEnabled(false)
                    }
                    onLoginSuccess()
                }
                is MoneyGuardResult.Failure -> {
                    preferenceManager?.saveMoneyguardEnabled(false)
                    onLoginSuccess()
                }
                is MoneyGuardResult.Loading -> {
                    // Loading state - do nothing
                }
            }
        }
    } catch (e: Exception) {
        preferenceManager?.saveMoneyguardEnabled(false)
        onLoginSuccess()
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginSuccess = {})
}