package ng.wimika.samplebankapp.local

interface IPreferenceManager {
    fun saveMoneyGuardToken(token: String?)
    fun getMoneyGuardToken(): String?

    fun saveUserFirstName(firstName: String?)
    fun getUserFirstName(): String?

    // Bank login details
    fun saveBankLoginDetails(sessionId: String?, userFullName: String?)
    fun getBankSessionId(): String?
    fun getBankUserFullName(): String?

    // Moneyguard enabled status
    fun saveMoneyguardEnabled(enabled: Boolean)
    fun isMoneyguardEnabled(): Boolean

    // User names from Moneyguard
    fun saveMoneyguardUserNames(firstName: String?, lastName: String?)
    fun getMoneyguardFirstName(): String?
    fun getMoneyguardLastName(): String?

    fun clear()
}