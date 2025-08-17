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
    object OnLogout : LoginEvent
}

sealed interface LoginSideEffect {
    object NavigateToDashboard : LoginSideEffect
    object NavigateToVerification : LoginSideEffect
    data class ShowRiskDialog(val risk: SpecificRisk) : LoginSideEffect
    object HideRiskDialog : LoginSideEffect
    data class ShowCredentialDialog(val status: String) : LoginSideEffect
    object ShowUnusualLocationDialog : LoginSideEffect
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

    private val moneyGuardPrelaunch: MoneyGuardPrelaunch? = sdkService?.prelaunch()

    init {
        _uiState.update { it.copy(isDebugLogsEnabled = preferenceManager?.isDebugLogsEnabled() ?: false) }
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
                val startupRisk = moneyGuardPrelaunch?.startup()
                val risks = startupRisk?.risks?.filter {
                    it.status == RiskStatus.RISK_STATUS_WARN || it.status == RiskStatus.RISK_STATUS_UNSAFE
                } ?: emptyList()

                _uiState.update { it.copy(prelaunchRisks = risks, isPrelaunchChecking = false) }

                if (risks.isNotEmpty()) {
                    _sideEffect.send(LoginSideEffect.ShowRiskDialog(risks.first()))
                }
            } catch (e: Exception) {
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

                // Step 2: MoneyGuard Registration
                val registrationResult = registerWithMoneyGuard(sessionData.sessionId)
                if (registrationResult == RegistrationResult.NEEDS_VERIFICATION) {
                    _sideEffect.send(LoginSideEffect.NavigateToVerification)
                    return@launch
                }

                // Step 3: Post-Login Flow (Credential & Location Checks)
                handlePostLoginFlow()

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Login failed. Please check your credentials.", isLoading = false) }
            }
        }
    }

    private suspend fun registerWithMoneyGuard(sessionId: String): RegistrationResult {
        return try {
            val resultFlow = sdkService?.authentication()?.register(
                parteBankId = Constants.PARTNER_BANK_ID,
                partnerSessionToken = sessionId
            )

            val finalResult = resultFlow?.first { it !is MoneyGuardResult.Loading }

            when (finalResult) {
                is MoneyGuardResult.Success -> {
                    val response = finalResult.data
                    if (response.token.isNotEmpty()) {
                        preferenceManager?.saveMoneyGuardToken(response.token)
                        preferenceManager?.saveMoneyGuardInstallationId(response.installationId)
                        preferenceManager?.saveMoneyguardUserNames(response.userDetails.firstName, response.userDetails.lastName)
                    }
                    if (response.result == SessionResultFlags.UntrustedInstallationRequires2Fa
                        && sdkService?.utility()?.checkMoneyguardPolicyStatus(response.token) == MoneyGuardAppStatus.Active
                    ) {
                        RegistrationResult.NEEDS_VERIFICATION
                    } else {
                        RegistrationResult.SUCCESS
                    }
                }
                is MoneyGuardResult.Failure -> RegistrationResult.SUCCESS // Fail open: proceed even if registration fails
                else -> RegistrationResult.SUCCESS // Fail open
            }
        } catch (e: Exception) {
            RegistrationResult.SUCCESS // Fail open
        }
    }

    private fun handlePostLoginFlow() {
        viewModelScope.launch {
            val token = preferenceManager?.getMoneyGuardToken()
            if (sdkService == null || token.isNullOrEmpty()) {
                _sideEffect.send(LoginSideEffect.NavigateToDashboard)
                return@launch
            }

            val status = sdkService.utility()?.checkMoneyguardPolicyStatus(token)
            if (status == MoneyGuardAppStatus.Active) {
                performCredentialCheck(token)
            } else {
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

                sdkService?.authentication()?.credentialCheck(token, credential) { result ->
                    val statusText = if (result is MoneyGuardResult.Success) {
                        if (result.data.status == RiskStatus.RISK_STATUS_UNSAFE) {
                            preferenceManager?.setIdentityCompromised(true)
                        }
                        "Credential Check - ${result.data.status}"
                    }
                    else {
                        "Credential Check - Could not determine status"
                    }
                    viewModelScope.launch {
                        _sideEffect.send(
                            LoginSideEffect.ShowCredentialDialog(
                                statusText
                            )
                        )
                    }
                }
            } catch (e: Exception) {
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
                val response = sdkService?.utility()?.checkLocation(token)

                //Use joinToString to format the list with a newline for each item
                val formattedList = response?.data?.joinToString(separator = "\n") { item ->
                    "  - $item" // 'item' will use the data class's automatic toString()
                } ?: "null" // Handle the case where the list itself is null

                Log.i(LOG_TAG, "[SampleBankApp|LoginviewModel] Location check:\n$formattedList")
                if (response?.data?.isNotEmpty() == true) {
                    _sideEffect.send(LoginSideEffect.ShowUnusualLocationDialog)
                } else {
                    _sideEffect.send(LoginSideEffect.NavigateToDashboard)
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "[SampleBankApp|LoginviewModel] ❌ Error during location check: ${e.message}")
                // Failsafe: proceed to dashboard if location check fails
                _sideEffect.send(LoginSideEffect.NavigateToDashboard)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun resetLoginState() {
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