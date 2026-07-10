package ng.wimika.samplebankapp.ui.screens.Login

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ng.wimika.moneyguard_sdk.services.prelaunch.MoneyGuardPrelaunch
import ng.wimika.moneyguard_sdk.services.utility.MoneyGuardAppStatus
import ng.wimika.moneyguard_sdk_auth.datasource.auth_service.models.credential.Credential
import ng.wimika.moneyguard_sdk_auth.datasource.auth_service.models.credential.HashAlgorithm
import ng.wimika.moneyguard_sdk_commons.types.MoneyGuardResult
import ng.wimika.moneyguard_sdk_commons.types.RiskStatus
import ng.wimika.moneyguard_sdk_commons.types.SessionResultFlags
import ng.wimika.moneyguard_sdk_commons.types.SpecificRisk
import ng.wimika.samplebankapp.Constants
import ng.wimika.samplebankapp.MoneyGuardClientApp
import ng.wimika.samplebankapp.local.IPreferenceManager
import ng.wimika.samplebankapp.loginRepo.LoginRepository
import ng.wimika.samplebankapp.loginRepo.LoginRepositoryImpl
import java.security.MessageDigest

// --- Data classes for State, Events, and Side Effects ---

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isDebugLogsEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val prelaunchRisks: List<SpecificRisk> = emptyList(),
    val currentRiskIndex: Int = 0,
    val isPrelaunchChecking: Boolean = true,
)

sealed interface LoginEvent {
    data class OnUsernameChange(val value: String) : LoginEvent
    data class OnPasswordChange(val value: String) : LoginEvent
    object OnTogglePasswordVisibility : LoginEvent
    data class OnDebugLogsToggle(val enabled: Boolean) : LoginEvent
    object OnLoginClick : LoginEvent
    object OnDismissRiskModal : LoginEvent
    object OnDismissCredentialDialog : LoginEvent
    object OnDismissUnusualLocationDialogAndVerify : LoginEvent
    object OnDismissUnusualLocationDialogAndProceed : LoginEvent
    object OnDismissUntrustedDeviceDialog : LoginEvent
    object OnDismissTypingVerificationFailedDialog : LoginEvent
    object OnLogout : LoginEvent
}

sealed interface LoginSideEffect {
    object NavigateToDashboard : LoginSideEffect
    object NavigateToVerification : LoginSideEffect
    data class ShowRiskDialog(val risk: SpecificRisk) : LoginSideEffect
    object HideRiskDialog : LoginSideEffect
    data class ShowCredentialDialog(val status: String) : LoginSideEffect
    object ShowUnusualLocationDialog : LoginSideEffect
    object ShowUntrustedDeviceDialog : LoginSideEffect
    data class ShowToast(val message: String) : LoginSideEffect
    data class ShowTypingVerificationFailedDialog(val message: String) : LoginSideEffect
}

class LoginViewModel(
    // In a real app, these would be injected by Hilt/Koin
    private val loginRepository: LoginRepository = LoginRepositoryImpl(),
    private val preferenceManager: IPreferenceManager? = MoneyGuardClientApp.preferenceManager,
    private val sdkService: ng.wimika.moneyguard_sdk.services.MoneyGuardSdkService? = MoneyGuardClientApp.sdkService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<LoginSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()
    private val LOG_TAG = "MONEYGUARD_LOGGER"
    private val SDK_TAG = "MG_SDK_TRACE"

    private val moneyGuardPrelaunch: MoneyGuardPrelaunch? = sdkService?.prelaunch()
    //private val typingProfileService = sdkService?.getTypingProfile()

    init {
        val isLoggedOut = preferenceManager?.getIsLoggedOut() ?: false
        _uiState.update { 
            it.copy(
                isDebugLogsEnabled = preferenceManager?.isDebugLogsEnabled() ?: false,
                isPrelaunchChecking = !isLoggedOut // Don't show loading if user explicitly logged out
            ) 
        }
        runPrelaunchChecks()
    }

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnUsernameChange -> _uiState.update { it.copy(username = event.value) }
            is LoginEvent.OnPasswordChange -> _uiState.update { it.copy(password = event.value) }
            is LoginEvent.OnTogglePasswordVisibility -> _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is LoginEvent.OnDebugLogsToggle -> {
                _uiState.update { it.copy(isDebugLogsEnabled = event.enabled) }
                preferenceManager?.saveDebugLogsEnabled(event.enabled)
            }
            is LoginEvent.OnLoginClick -> login()
            is LoginEvent.OnDismissRiskModal -> handleRiskDismissal()
            is LoginEvent.OnDismissCredentialDialog -> performLocationCheck()
            is LoginEvent.OnDismissUnusualLocationDialogAndVerify -> viewModelScope.launch { _sideEffect.send(LoginSideEffect.NavigateToVerification) }
            is LoginEvent.OnDismissUnusualLocationDialogAndProceed -> {
                preferenceManager?.saveSuspiciousLoginStatus(true)
                viewModelScope.launch { _sideEffect.send(LoginSideEffect.NavigateToDashboard) }
            }
            is LoginEvent.OnDismissUntrustedDeviceDialog -> viewModelScope.launch { _sideEffect.send(LoginSideEffect.NavigateToVerification) }
            is LoginEvent.OnDismissTypingVerificationFailedDialog -> {
                // User chose to proceed anyway after failed verification
                handlePostLoginFlow()
            }
            is LoginEvent.OnLogout -> resetLoginState()
        }
    }

    private fun runPrelaunchChecks() {
        if (preferenceManager?.getIsLoggedOut() == true) {
            _uiState.update { it.copy(isPrelaunchChecking = false) }
            return // Don't run checks if user explicitly logged out
        }

        viewModelScope.launch {
            try {
                Log.d(SDK_TAG, "━━━ prelaunch().startup() ━━━")
                Log.d(SDK_TAG, "  ➡️ PARAMS: (none — passive device/network scan)")
                val startupRisk = moneyGuardPrelaunch?.startup()
                Log.d(SDK_TAG, "  ⬅️ RESULT: startupRisk=${startupRisk}")
                Log.d(SDK_TAG, "  ⬅️ RESULT: risks count=${startupRisk?.risks?.size ?: 0}")
                startupRisk?.risks?.forEachIndexed { i, risk ->
                    Log.d(SDK_TAG, "  ⬅️ RESULT: risk[$i] name=${risk.name}, status=${risk.status}, details=${risk.additionalDetails}")
                }
                val risks = startupRisk?.risks?.filter {
                    it.status == RiskStatus.RISK_STATUS_WARN || it.status == RiskStatus.RISK_STATUS_UNSAFE
                } ?: emptyList()
                Log.d(SDK_TAG, "  ⬅️ FILTERED: ${risks.size} risks with WARN/UNSAFE status")

                _uiState.update { it.copy(prelaunchRisks = risks, isPrelaunchChecking = false) }

                // Save prelaunch risks to risk register
                if (risks.isNotEmpty()) {
                    preferenceManager?.clearRiskRegister() // Clear old risks first
                    risks.forEach { risk ->
                        preferenceManager?.saveRiskToRegister(risk.name)
                    }
                    _sideEffect.send(LoginSideEffect.ShowRiskDialog(risks.first()))
                }
            } catch (e: Exception) {
                Log.e(SDK_TAG, "  ❌ prelaunch().startup() EXCEPTION: ${e.message}", e)
                _uiState.update { it.copy(isPrelaunchChecking = false) }
            }
        }
    }

    private fun handleRiskDismissal() {
        val currentIdx = _uiState.value.currentRiskIndex
        val risks = _uiState.value.prelaunchRisks
        val nextIndex = currentIdx + 1

        if (nextIndex < risks.size) {
            _uiState.update { it.copy(currentRiskIndex = nextIndex) }
            viewModelScope.launch { _sideEffect.send(LoginSideEffect.ShowRiskDialog(risks[nextIndex])) }
        } else {
            viewModelScope.launch { _sideEffect.send(LoginSideEffect.HideRiskDialog) }
        }
    }

    private fun login() {
        val currentState = _uiState.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Step 1: Bank Login
                val loginResponse = loginRepository.login(
                     email = currentState.username.trim(),
                    password = currentState.password,
                    appVersion = Constants.APP_VERSION,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    androidVersion = Build.VERSION.RELEASE
                ).first() // Assuming collect is not needed and we just want the first emission

                val sessionData = loginResponse.data
                if (sessionData == null || sessionData.sessionId.isEmpty()) {
                    throw IllegalStateException("Login failed: Invalid session data.")
                }

                preferenceManager?.saveBankLoginDetails(sessionData.sessionId, sessionData.userFullName)
                preferenceManager?.saveUserEmail(currentState.username.trim())
                preferenceManager?.saveSuspiciousLoginStatus(false)
                preferenceManager?.saveLoggedOut(false) // Clear logged out flag on successful login

                // Step 2: Check if user is a MoneyGuard customer
                val isCustomer = try {
                    Log.d(SDK_TAG, "━━━ policy().isCustomer() ━━━")
                    Log.d(SDK_TAG, "  ➡️ PARAMS: userId=${sessionData.sessionId.take(8)}..., partnerId=${Constants.PARTNER_BANK_ID}")
                    val result = sdkService?.policy()?.isCustomer(sessionData.sessionId, Constants.PARTNER_BANK_ID)
                    val customerResult = result?.getOrNull() ?: false
                    Log.d(SDK_TAG, "  ⬅️ RESULT: isCustomer=$customerResult")
                    customerResult
                } catch (e: Exception) {
                    Log.e(SDK_TAG, "  ❌ policy().isCustomer() EXCEPTION: ${e.message}", e)
                    true // Fail open: assume customer and proceed with registration
                }

                if (!isCustomer) {
                    Log.d(SDK_TAG, "  ➡️ User is NOT a MoneyGuard customer, skipping registration → handlePostLoginFlow")
                    handlePostLoginFlow()
                    return@launch
                }

                // Step 3: MoneyGuard Registration (only if user is a customer)
                val registrationResult = registerWithMoneyGuard(sessionData.sessionId)
                if (registrationResult == RegistrationResult.NEEDS_VERIFICATION) {
                    _sideEffect.send(LoginSideEffect.ShowUntrustedDeviceDialog)
                    return@launch
                }

                // Step 3.5: Typing Pattern Check (Enrollment or Verification)
                //handleTypingPatternCheck()

                // Step 4: Post-Login Flow (Credential & Location Checks)
                // This will be called after typing pattern check completes
                 handlePostLoginFlow()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Login failed. Please check your credentials.", isLoading = false) }
            }
        }
    }

    private suspend fun registerWithMoneyGuard(sessionId: String): RegistrationResult {
        return try {
            Log.d(SDK_TAG, "━━━ authentication().register() ━━━")
            Log.d(SDK_TAG, "  ➡️ PARAMS: partnerBankId=${Constants.PARTNER_BANK_ID}, partnerSessionToken=${sessionId.take(8)}...")
            val resultFlow = sdkService?.authentication()?.register(
                parteBankId = Constants.PARTNER_BANK_ID,
                partnerSessionToken = sessionId
            )

            val finalResult = resultFlow?.first { it !is MoneyGuardResult.Loading }
            Log.d(SDK_TAG, "  ⬅️ RESULT TYPE: ${finalResult?.javaClass?.simpleName}")

            when (finalResult) {
                is MoneyGuardResult.Success -> {
                    val response = finalResult.data
                    Log.d(SDK_TAG, "  ⬅️ RESULT: hasToken=${response.token.isNotEmpty()}, tokenLength=${response.token.length}")
                    Log.d(SDK_TAG, "  ⬅️ RESULT: installationId=${response.installationId}")
                    Log.d(SDK_TAG, "  ⬅️ RESULT: firstName=${response.userDetails.firstName}, lastName=${response.userDetails.lastName}")
                    Log.d(SDK_TAG, "  ⬅️ RESULT: highRiskThreshold=${response.highRiskThreshold}")
                    Log.d(SDK_TAG, "  ⬅️ RESULT: sessionResultFlag=${response.result}")
                    if (response.token.isNotEmpty()) {
                        preferenceManager?.saveMoneyGuardToken(response.token)
                        preferenceManager?.saveMoneyGuardInstallationId(response.installationId)
                        preferenceManager?.saveMoneyguardUserNames(response.userDetails.firstName, response.userDetails.lastName)
                        preferenceManager?.saveHighRiskThreshold(response.highRiskThreshold)
                    }
                    if (response.result == SessionResultFlags.UntrustedInstallationRequires2Fa) {
                        Log.d(SDK_TAG, "━━━ utility().checkMoneyguardPolicyStatus() [inside register] ━━━")
                        Log.d(SDK_TAG, "  ➡️ PARAMS: hasToken=${response.token.isNotEmpty()}, tokenLength=${response.token.length}")
                        val policyStatus = sdkService?.utility()?.checkMoneyguardPolicyStatus(response.token)
                        Log.d(SDK_TAG, "  ⬅️ RESULT: policyStatus=$policyStatus")
                        if (policyStatus == MoneyGuardAppStatus.Active) {
                            RegistrationResult.NEEDS_VERIFICATION
                        } else {
                            RegistrationResult.SUCCESS
                        }
                    } else {
                        RegistrationResult.SUCCESS
                    }
                }
                is MoneyGuardResult.Failure -> {
                    Log.e(SDK_TAG, "  ❌ RESULT: registration failed: ${finalResult.error.message}")
                    RegistrationResult.SUCCESS // Fail open: proceed even if registration fails
                }
                else -> {
                    Log.w(SDK_TAG, "  ⚠️ RESULT: unexpected result type: $finalResult")
                    RegistrationResult.SUCCESS // Fail open
                }
            }
        } catch (e: Exception) {
            Log.e(SDK_TAG, "  ❌ authentication().register() EXCEPTION: ${e.message}", e)
            RegistrationResult.SUCCESS // Fail open
        }
    }

    private fun handlePostLoginFlow() {
        viewModelScope.launch {
            val token = preferenceManager?.getMoneyGuardToken()
            if (sdkService == null || token.isNullOrEmpty()) {
                Log.d(SDK_TAG, "━━━ handlePostLoginFlow: skipping SDK checks (sdk=${sdkService != null}, token=${!token.isNullOrEmpty()}) ━━━")
                _sideEffect.send(LoginSideEffect.NavigateToDashboard)
                return@launch
            }

            Log.d(SDK_TAG, "━━━ utility().checkMoneyguardPolicyStatus() [post-login] ━━━")
            Log.d(SDK_TAG, "  ➡️ PARAMS: hasToken=${token.isNotEmpty()}, tokenLength=${token.length}")
            val status = sdkService.utility()?.checkMoneyguardPolicyStatus(token)
            Log.d(SDK_TAG, "  ⬅️ RESULT: policyStatus=$status")
            if (status == MoneyGuardAppStatus.Active) {
                performCredentialCheck(token)
            } else {
                Log.d(SDK_TAG, "  ➡️ Policy not active, skipping credential/location checks → Dashboard")
                _sideEffect.send(LoginSideEffect.NavigateToDashboard)
            }
        }
    }

    private fun performCredentialCheck(token: String) {
        viewModelScope.launch {
            try {
                val passwordLast3Chars = _uiState.value.password.takeLast(3)
                val hashedPasswordLast3 = sha256Hash(passwordLast3Chars)
                val credential = Credential(
                    username = _uiState.value.username.trim(),
                    passwordStartingCharactersHash = hashedPasswordLast3,
                    domain = "wimika.ng",
                    hashAlgorithm = HashAlgorithm.SHA256
                )

                Log.d(SDK_TAG, "━━━ authentication().credentialCheck() ━━━")
                Log.d(SDK_TAG, "  ➡️ PARAMS: token=${token.take(12)}...")
                Log.d(SDK_TAG, "  ➡️ PARAMS: credential check requested, hasUsername=${credential.username.isNotBlank()}, hasDomain=${credential.domain.isNotBlank()}, hashAlgorithm=${credential.hashAlgorithm}")

                sdkService?.authentication()?.credentialCheck(token, credential) { result ->
                    Log.d(SDK_TAG, "  ⬅️ RESULT TYPE: ${result.javaClass.simpleName}")
                    when (result) {
                        is MoneyGuardResult.Loading -> {
                            Log.d(SDK_TAG, "  ⏳ RESULT: credential check loading, waiting...")
                            return@credentialCheck // Ignore intermediate loading state
                        }
                        is MoneyGuardResult.Success -> {
                            Log.d(SDK_TAG, "  ⬅️ RESULT: status=${result.data.status}")
                            if (result.data.status == RiskStatus.RISK_STATUS_UNSAFE) {
                                Log.w(SDK_TAG, "  ⚠️ RESULT: credential is UNSAFE — identity compromised")
                                preferenceManager?.setIdentityCompromised(true)
                                preferenceManager?.saveRiskToRegister("identity_compromise")
                            }
                            val statusText = "Credential Check - ${result.data.status}"
                            viewModelScope.launch {
                                _sideEffect.send(LoginSideEffect.ShowCredentialDialog(statusText))
                            }
                        }
                        else -> {
                            // Failure or unexpected result — fail silently and proceed
                            Log.w(SDK_TAG, "  ⚠️ RESULT: credential check returned non-success: $result — proceeding silently")
                            viewModelScope.launch { performLocationCheck() }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(SDK_TAG, "  ❌ authentication().credentialCheck() EXCEPTION: ${e.message}", e)
                // Failsafe: proceed to location check if credential check has an error
                performLocationCheck()
            }
        }
    }

    private fun performLocationCheck() {
        viewModelScope.launch {
            val token = preferenceManager?.getMoneyGuardToken() ?: run {
                _sideEffect.send(LoginSideEffect.NavigateToDashboard)
                return@launch
            }

            try {
                Log.d(SDK_TAG, "━━━ utility().checkLocation() ━━━")
                Log.d(SDK_TAG, "  ➡️ PARAMS: token=${token.take(12)}...")
                val response = sdkService?.utility()?.checkLocation(token)

                Log.d(SDK_TAG, "  ⬅️ RESULT: response=${response}")
                Log.d(SDK_TAG, "  ⬅️ RESULT: locations count=${response?.data?.size ?: 0}")
                response?.data?.forEachIndexed { i, item ->
                    Log.d(SDK_TAG, "  ⬅️ RESULT: location[$i]=$item")
                }

                //Use joinToString to format the list with a newline for each item
                val formattedList = response?.data?.joinToString(separator = "\n") { item ->
                    "  - $item" // 'item' will use the data class's automatic toString()
                } ?: "null" // Handle the case where the list itself is null

                Log.i(LOG_TAG, "[SampleBankApp|LoginviewModel] Location check:\n$formattedList")
                if (response?.data?.isNotEmpty() == true) {
                    Log.w(SDK_TAG, "  ⚠️ Unusual locations detected → showing dialog")
                    preferenceManager?.saveRiskToRegister("unusual_location")
                    _sideEffect.send(LoginSideEffect.ShowUnusualLocationDialog)
                } else {
                    Log.d(SDK_TAG, "  ✅ No unusual locations → Dashboard")
                    _sideEffect.send(LoginSideEffect.NavigateToDashboard)
                }
            } catch (e: Exception) {
                Log.e(SDK_TAG, "  ❌ utility().checkLocation() EXCEPTION: ${e.message}", e)
                Log.e(LOG_TAG, "[SampleBankApp|LoginviewModel] ❌ Error during location check: ${e.message}")
                // Failsafe: proceed to dashboard if location check fails
                _sideEffect.send(LoginSideEffect.NavigateToDashboard)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun resetLoginState() {
        // Clear risk register on logout
        preferenceManager?.clearRiskRegister()
        
        _uiState.update {
            it.copy(
                username = "",
                password = "",
                isPasswordVisible = false,
                isLoading = false,
                errorMessage = null,
                prelaunchRisks = emptyList(),
                currentRiskIndex = 0,
                isPrelaunchChecking = false, // Start with false to avoid immediate loading state
                isDebugLogsEnabled = preferenceManager?.isDebugLogsEnabled() ?: false
            )
        }
        // Defer prelaunch checks to avoid blocking logout
        viewModelScope.launch {
            kotlinx.coroutines.delay(100) // Small delay to let logout complete smoothly
            _uiState.update { it.copy(isPrelaunchChecking = true) }
            runPrelaunchChecks()
        }
    }

    private fun handleTypingPatternCheck() {
//        viewModelScope.launch {
//            try {
//                val username = _uiState.value.username.trim()
//                val token = preferenceManager?.getMoneyGuardToken()
//
//                if (typingProfileService == null || token.isNullOrEmpty()) {
//                    Log.w(LOG_TAG, "[SampleBankApp|LoginViewModel] Typing profile service or token not available, skipping typing pattern check")
//                    handlePostLoginFlow()
//                    return@launch
//                }
//
//                Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] Checking if user is enrolled for typing pattern")
//
//                // Check if user is enrolled
//
//                val isEnrolledResult = typingProfileService.isEnrolled("auth", token)
//
//                Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] isEnrolled result: success=${isEnrolledResult.success}, isEnrolled=${isEnrolledResult.isEnrolled}, message=${isEnrolledResult.message}")
//
//                if (isEnrolledResult.success && isEnrolledResult.isEnrolled) {
//                    // User is enrolled, perform verification
//                    Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] User is enrolled, performing typing pattern verification")
//                    performTypingPatternVerification(username, token)
//                } else {
//                    // User is not enrolled, perform enrollment
//                    Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] User is not enrolled, performing typing pattern enrollment")
//                    performTypingPatternEnrollment(username, token)
//                }
//
//                typingProfileService.resetService()
//                typingProfileService.stopService()
//            } catch (e: Exception) {
//                Log.e(LOG_TAG, "[SampleBankApp|LoginViewModel] ❌ Error during typing pattern check: ${e.message}", e)
//                // Proceed with login flow even if typing pattern check fails
//                handlePostLoginFlow()
//            }
//        }
    }

    private fun performTypingPatternEnrollment(username: String, token: String) {
//        viewModelScope.launch {
//            try {
//                Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] Starting typing pattern enrollment for user: $username")
//
//                val result = typingProfileService?.saveTypingProfileForAuth(username, token)
//
//                Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] Enrollment result: success=${result?.success}, message=${result?.message}, enrollment=${result?.enrollment}")
//
//                if (result?.success == true) {
//                    val enrollmentProgress = result.enrollment ?: 0
//                    val message = when {
//                        enrollmentProgress >= 3 -> "Behavioural Biometrics data enrollment completed! ✓"
//                        else -> "Behavioural Biometrics data captured for enrollment)"
//                    }
//                    Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] ✓ $message")
//                    _sideEffect.send(LoginSideEffect.ShowToast(message))
//                } else {
//                    Log.w(LOG_TAG, "[SampleBankApp|LoginViewModel] Typing pattern enrollment failed: ${result?.message}")
//                    _sideEffect.send(LoginSideEffect.ShowToast("Behavioral Biometrics capture failed"))
//                }
//                typingProfileService?.resetService()
//                typingProfileService?.stopService()
//            } catch (e: Exception) {
//                Log.e(LOG_TAG, "[SampleBankApp|LoginViewModel] ❌ Exception during typing pattern enrollment: ${e.message}", e)
//            } finally {
//                // Enrollment is non-blocking, proceed with login flow
//                handlePostLoginFlow()
//            }
//        }
    }

    private fun performTypingPatternVerification(username: String, token: String) {
//        viewModelScope.launch {
//            try {
//                Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] Starting typing pattern verification for user: $username")
//
//                val result = typingProfileService?.verifyTypingProfileForAuth(username, token)
//
//                Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] Verification result: success=${result?.success}, matched=${result?.matched}, message=${result?.message}, asString=${result?.asString}")
//
//                if (result?.success == true && result.matched) {
//                    // Verification successful
//                    Log.i(LOG_TAG, "[SampleBankApp|LoginViewModel] ✓ Typing pattern verification successful")
//                    _sideEffect.send(LoginSideEffect.ShowToast("Behavioral Biometrics verified successfully! ✓"))
//                    handlePostLoginFlow()
//                } else {
//                    // Verification failed - show blocking dialog
//                    val failureMessage = result?.message ?: "Typing pattern did not match"
//                    Log.w(LOG_TAG, "[SampleBankApp|LoginViewModel] ❌ Typing pattern verification failed: $failureMessage")
//                    _sideEffect.send(LoginSideEffect.ShowTypingVerificationFailedDialog(failureMessage))
//                    _uiState.update { it.copy(isLoading = false) }
//                }
//
//                typingProfileService?.resetService()
//                typingProfileService?.stopService()
//            } catch (e: Exception) {
//                Log.e(LOG_TAG, "[SampleBankApp|LoginViewModel] ❌ Exception during typing pattern verification: ${e.message}", e)
//                // On exception, show failure dialog
//                _sideEffect.send(LoginSideEffect.ShowTypingVerificationFailedDialog("Error verifying typing pattern: ${e.message}"))
//                _uiState.update { it.copy(isLoading = false) }
//            }
//        }
    }

    companion object {
        fun sha256Hash(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun getRiskMessage(risk: SpecificRisk): String {
            return when (risk.name) {
                SpecificRisk.SPECIFIC_RISK_DEVICE_SECURITY_MISCONFIGURATION_NAME -> {
                    "USB debugging is enabled on your device. Your login credentials may be compromised."
                }
                SpecificRisk.SPECIFIC_RISK_NETWORK_WIFI_ENCRYPTION_NAME,
                SpecificRisk.SPECIFIC_RISK_NETWORK_WIFI_PASSWORD_PROTECTION_NAME -> {
                    "Unsecured WiFi detected. Your digital banking activities may be compromised."
                }
                SpecificRisk.SPECIFIC_RISK_DEVICE_ROOT_OR_JAILBREAK_NAME -> {
                    "Device security is compromised. We strongly advise you not to log into your bank app."
                }
                SpecificRisk.SPECIFIC_RISK_NETWORK_DNS_SPOOFING_NAME -> {
                    "DNS spoofing detected. Your banking activities are at risk if you continue."
                }
                SpecificRisk.SPECIFIC_RISK_DEVICE_VULNERABILITY_NAME -> {
                    "Device vulnerabilities detected. Please update your device for better security."
                }
                SpecificRisk.SPECIFIC_RISK_NETWORK_MITM_NAME -> {
                    "Man-in-the-middle attack detected. Your connection may be compromised."
                }
                SpecificRisk.SPECIFIC_RISK_USER_IDENTITY_COMPROMISE_NAME -> {
                    "Identity compromise detected. Please verify your identity before proceeding."
                }
                SpecificRisk.SPECIFIC_RISK_APPLICATION_PHISHING_NAME -> {
                    "Phishing attempt detected. Please ensure you're using the official app."
                }
                SpecificRisk.SPECIFIC_RISK_APPLICATION_MALWARE_NAME -> {
                    "Malware detected on your device. Please scan and remove before proceeding."
                }
                SpecificRisk.SPECIFIC_RISK_APPLICATION_FAKE_APPS_NAME -> {
                    "Fake app detected. Please ensure you're using the official banking app."
                }
                SpecificRisk.SPECIFIC_RISK_APPLICATION_KEY_LOGGING_NAME -> {
                    "Key logging detected. Your keystrokes may be monitored."
                }
                SpecificRisk.SPECIFIC_RISK_USER_SECURITY_AWARENESS_NAME -> {
                    "Security awareness issue detected. Please review your security practices."
                }
                else -> {
                    risk.additionalDetails ?: "Security risk detected: ${risk.name}"
                }
            }
        }
    }
}

private enum class RegistrationResult {
    SUCCESS,
    NEEDS_VERIFICATION
}
