package ng.wimika.moneyguardsdkclient.ui.features.moneyguard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy

class MoneyGuardViewModel(
    private val moneyGuardPolicy: MoneyGuardPolicy,
    private val token: String
) : ViewModel() {
    private val _bankAccounts = MutableStateFlow<List<BankAccount>>(emptyList())
    val bankAccounts: StateFlow<List<BankAccount>> = _bankAccounts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchBankAccounts()
    }

    private fun fetchBankAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                moneyGuardPolicy.getUserAccounts(token, 101).fold(
                    onSuccess = { response ->
                        _bankAccounts.value = response.bankAccounts
                    },
                    onFailure = { error ->
                        _error.value = error.message
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPolicy(
        policyOptionId: String,
        coveredAccountIds: List<String>,
        debitAccountId: String,
        autoRenew: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isSuccess.value = false
            _successMessage.value = null

            try {
                moneyGuardPolicy.createPolicy(
                    token = token,
                    policyOptionId = policyOptionId,
                    coveredAccountIds = coveredAccountIds,
                    debitAccountId = debitAccountId,
                    autoRenew = autoRenew
                ).fold(
                    onSuccess = { response ->
                        _isSuccess.value = true
                        _successMessage.value = "Policy created successfully!"
                        _error.value = null
                    },
                    onFailure = { error ->
                        _isSuccess.value = false
                        _successMessage.value = null
                        _error.value = error.message
                    }
                )
            } catch (e: Exception) {
                _isSuccess.value = false
                _successMessage.value = null
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
} 