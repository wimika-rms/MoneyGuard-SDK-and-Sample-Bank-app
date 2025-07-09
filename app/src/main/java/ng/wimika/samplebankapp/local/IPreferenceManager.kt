package ng.wimika.samplebankapp.local

interface IPreferenceManager {
    fun setIsFirstLaunchFlag()
    fun getIsFirstLaunchFlag(): Boolean?

    fun saveMoneyGuardToken(token: String?)
    fun getMoneyGuardToken(): String?

    fun saveUserFirstName(firstName: String?)
    fun getUserFirstName(): String?

    // Bank login details
    fun saveBankLoginDetails(sessionId: String?, userFullName: String?)
    fun getBankSessionId(): String?
    fun getBankUserFullName(): String?

    // User names from Moneyguard
    fun saveMoneyguardUserNames(firstName: String?, lastName: String?)
    fun getMoneyguardFirstName(): String?
    fun getMoneyguardLastName(): String?

    // MoneyGuard setup preferences
    fun saveMoneyGuardSetupPreferences(preferences: MoneyGuardSetupPreferences?)
    fun getMoneyGuardSetupPreferences(): MoneyGuardSetupPreferences?
    
    // New methods for suspicious login
    fun saveSuspiciousLoginStatus(isSuspicious: Boolean)
    fun isSuspiciousLogin(): Boolean

    fun clear()
}

data class MoneyGuardSetupPreferences(
    val accountIds: List<String> = emptyList(),
    val coverageLimitId: String = "",
    val amountToCover: String = "",
    val policyOptionId: String = "",
    val subscriptionPlan: String = "",
    val debitAccount: String = "",
    val autoRenew: Boolean = true
)