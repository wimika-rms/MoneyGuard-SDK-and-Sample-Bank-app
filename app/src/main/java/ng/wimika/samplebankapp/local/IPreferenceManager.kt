package ng.wimika.samplebankapp.local

import androidx.core.content.edit

interface IPreferenceManager {
    fun saveMoneyGuardToken(token: String?)
    fun getMoneyGuardToken(): String?

    fun setIdentityCompromised(compromised: Boolean)
    fun isIdentityCompromised(): Boolean?

    fun saveMoneyGuardInstallationId(token: String?)
    fun getMoneyGuardInstallationId(): String?

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

    // Debug logs preference
    fun saveDebugLogsEnabled(enabled: Boolean)
    fun isDebugLogsEnabled(): Boolean

    // User email preference
    fun saveUserEmail(email: String?)
    fun getUserEmail(): String?

    fun saveLoggedOut(state : Boolean)
    fun getIsLoggedOut(): Boolean

    // App lifecycle management for security
    fun markAppStarted()
    fun markAppProperlyClosed()
    fun wasAppForceClosedPreviously(): Boolean
    fun clearAllOnAppClose()

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