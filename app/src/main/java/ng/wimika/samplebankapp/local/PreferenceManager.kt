package ng.wimika.samplebankapp.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferenceManager(private val context: Context): IPreferenceManager {

    companion object {
        private const val MONEY_GUARD_TOKEN = "moneyguard_token"
        private const val USER_FIRST_NAME = "user_first_name"
        
        // Bank login details
        private const val BANK_SESSION_ID = "bank_session_id"
        private const val BANK_USER_FULL_NAME = "bank_user_full_name"
        
        // Moneyguard enabled status
        private const val MONEYGUARD_ENABLED = "moneyguard_enabled"
        
        // User names from Moneyguard
        private const val MONEYGUARD_FIRST_NAME = "moneyguard_first_name"
        private const val MONEYGUARD_LAST_NAME = "moneyguard_last_name"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("moneyguard.client.preference", Context.MODE_PRIVATE)
    }

    override fun saveMoneyGuardToken(token: String?) {
        sharedPreferences.edit { putString(MONEY_GUARD_TOKEN, token) }
    }

    override fun getMoneyGuardToken(): String? {
        return sharedPreferences.getString(MONEY_GUARD_TOKEN, null)
    }

    override fun saveUserFirstName(firstName: String?) {
        sharedPreferences.edit { putString(USER_FIRST_NAME, firstName) }
    }

    override fun getUserFirstName(): String? {
        return sharedPreferences.getString(USER_FIRST_NAME, null)
    }

    // Bank login details
    override fun saveBankLoginDetails(sessionId: String?, userFullName: String?) {
        sharedPreferences.edit { 
            putString(BANK_SESSION_ID, sessionId)
            putString(BANK_USER_FULL_NAME, userFullName)
        }
    }

    override fun getBankSessionId(): String? {
        return sharedPreferences.getString(BANK_SESSION_ID, null)
    }

    override fun getBankUserFullName(): String? {
        return sharedPreferences.getString(BANK_USER_FULL_NAME, null)
    }

    // Moneyguard enabled status
    override fun saveMoneyguardEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(MONEYGUARD_ENABLED, enabled) }
    }

    override fun isMoneyguardEnabled(): Boolean {
        return sharedPreferences.getBoolean(MONEYGUARD_ENABLED, false)
    }

    // User names from Moneyguard
    override fun saveMoneyguardUserNames(firstName: String?, lastName: String?) {
        sharedPreferences.edit { 
            putString(MONEYGUARD_FIRST_NAME, firstName)
            putString(MONEYGUARD_LAST_NAME, lastName)
        }
    }

    override fun getMoneyguardFirstName(): String? {
        return sharedPreferences.getString(MONEYGUARD_FIRST_NAME, null)
    }

    override fun getMoneyguardLastName(): String? {
        return sharedPreferences.getString(MONEYGUARD_LAST_NAME, null)
    }

    override fun clear() {
        sharedPreferences.edit { clear() }
    }

}