package ng.wimika.samplebankapp.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.CoverageLimit
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.PolicyOption

class AccountProtectionFlowState {
    private var _selectedAccountIds by mutableStateOf<Set<String>>(emptySet())
    private var _allAccounts by mutableStateOf<List<BankAccount>>(emptyList())
    private var _selectedCoverageLimit by mutableStateOf<CoverageLimit?>(null)
    private var _amountToCover by mutableStateOf("")
    private var _allCoverageLimits by mutableStateOf<List<CoverageLimit>>(emptyList())
    private var _selectedPolicyOption by mutableStateOf<PolicyOption?>(null)
    private var _autoRenew by mutableStateOf(false)
    private var _allPolicyOptions by mutableStateOf<List<PolicyOption>>(emptyList())
    private var _selectedDebitAccount by mutableStateOf<BankAccount?>(null)
    
    val selectedAccountIds: Set<String> get() = _selectedAccountIds
    val allAccounts: List<BankAccount> get() = _allAccounts
    val selectedCoverageLimit: CoverageLimit? get() = _selectedCoverageLimit
    val amountToCover: String get() = _amountToCover
    val allCoverageLimits: List<CoverageLimit> get() = _allCoverageLimits
    val selectedPolicyOption: PolicyOption? get() = _selectedPolicyOption
    val autoRenew: Boolean get() = _autoRenew
    val allPolicyOptions: List<PolicyOption> get() = _allPolicyOptions
    val selectedDebitAccount: BankAccount? get() = _selectedDebitAccount
    
    fun setSelectedAccountIds(accountIds: Set<String>) {
        _selectedAccountIds = accountIds
    }
    
    fun setAllAccounts(accounts: List<BankAccount>) {
        _allAccounts = accounts
    }
    
    // Coverage Limit Methods
    fun setSelectedCoverageLimit(coverageLimit: CoverageLimit?) {
        _selectedCoverageLimit = coverageLimit
    }
    
    fun setAmountToCover(amount: String) {
        _amountToCover = amount
    }
    
    fun setAllCoverageLimits(coverageLimits: List<CoverageLimit>) {
        _allCoverageLimits = coverageLimits
    }
    
    fun setSelectedPolicyOption(policyOption: PolicyOption?) {
        _selectedPolicyOption = policyOption
    }
    
    fun setAutoRenew(autoRenewEnabled: Boolean) {
        _autoRenew = autoRenewEnabled
    }
    
    fun setAllPolicyOptions(policyOptions: List<PolicyOption>) {
        _allPolicyOptions = policyOptions
    }
    
    fun setSelectedDebitAccount(account: BankAccount?) {
        _selectedDebitAccount = account
    }
    
    fun clearState() {
        _selectedAccountIds = emptySet()
        _allAccounts = emptyList()
        _selectedCoverageLimit = null
        _amountToCover = ""
        _allCoverageLimits = emptyList()
        _selectedPolicyOption = null
        _autoRenew = false
        _allPolicyOptions = emptyList()
        _selectedDebitAccount = null
    }
    
    fun hasAccountSelection(): Boolean = selectedAccountIds.isNotEmpty()
    
    fun hasCoverageLimitSelection(): Boolean = selectedCoverageLimit != null
    
    fun hasPolicyOptionSelection(): Boolean = selectedPolicyOption != null
    
    fun hasDebitAccountSelection(): Boolean = selectedDebitAccount != null
    
    fun getSubscriptionPlanDisplay(): String {
        return selectedPolicyOption?.let { option ->
            "${option.priceAndTerm.price}/${option.priceAndTerm.term}"
        } ?: ""
    }
} 