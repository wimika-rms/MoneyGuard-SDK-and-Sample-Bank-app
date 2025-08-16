package ng.wimika.samplebankapp.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson

class PreferenceManager(private val context: Context): IPreferenceManager {

    companion object {
        private const val IS_FIRST_LAUNCH = "is_first_launch"

        private const val MONEY_GUARD_TOKEN = "moneyguard_token"
        private const val IDENTITY_COMPROMISED = "identity_compromised"
        private const val MONEY_GUARD_INSTALLATION_ID = "moneyguard_installation_id"
        private const val USER_FIRST_NAME = "user_first_name"
        
        // Bank login details
        private const val BANK_SESSION_ID = "bank_session_id"
        private const val BANK_USER_FULL_NAME = "bank_user_full_name"
        
        // Moneyguard enabled status
        private const val MONEYGUARD_ENABLED = "moneyguard_enabled"
        
        // User names from Moneyguard
        private const val MONEYGUARD_FIRST_NAME = "moneyguard_first_name"
        private const val MONEYGUARD_LAST_NAME = "moneyguard_last_name"
        
        // MoneyGuard setup preferences
        private const val MONEYGUARD_SETUP_PREFERENCES = "moneyguard_setup_preferences"
        
        // New constant for suspicious login
        private const val SUSPICIOUS_LOGIN_STATUS = "suspicious_login_status"
        
        // Debug logs preference
        private const val DEBUG_LOGS_ENABLED = "debug_logs_enabled"
        
        // User email preference
        private const val USER_EMAIL = "user_email"

        private const val LOGGED_OUT = "logged_out"
        
        // App lifecycle constants for security
        private const val APP_STARTED = "app_started"
        private const val APP_PROPERLY_CLOSED = "app_properly_closed"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("moneyguard.client.preference", Context.MODE_PRIVATE)
    }
    
    private val gson = Gson()

    override fun setIdentityCompromised(compromised: Boolean) {
        sharedPreferences.edit { putBoolean(IDENTITY_COMPROMISED, compromised) }
    }

    override fun isIdentityCompromised(): Boolean? {
        return sharedPreferences.getBoolean(IDENTITY_COMPROMISED, false)
    }

    override fun saveMoneyGuardToken(token: String?) {
        sharedPreferences.edit { putString(MONEY_GUARD_TOKEN, token) }
    }

    override fun saveMoneyGuardInstallationId(token: String?) {
        sharedPreferences.edit { putString(MONEY_GUARD_INSTALLATION_ID, token) }
    }

    override fun getMoneyGuardInstallationId(): String? {
        return sharedPreferences.getString(MONEY_GUARD_INSTALLATION_ID, null)
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

    // MoneyGuard setup preferences
    override fun saveMoneyGuardSetupPreferences(preferences: MoneyGuardSetupPreferences?) {
        val json = gson.toJson(preferences)
        sharedPreferences.edit { putString(MONEYGUARD_SETUP_PREFERENCES, json) }
    }

    override fun getMoneyGuardSetupPreferences(): MoneyGuardSetupPreferences? {
        val json = sharedPreferences.getString(MONEYGUARD_SETUP_PREFERENCES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, MoneyGuardSetupPreferences::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // New methods implementation
    override fun saveSuspiciousLoginStatus(isSuspicious: Boolean) {
        sharedPreferences.edit { putBoolean(SUSPICIOUS_LOGIN_STATUS, isSuspicious) }
    }

    override fun isSuspiciousLogin(): Boolean {
        return sharedPreferences.getBoolean(SUSPICIOUS_LOGIN_STATUS, false)
    }

    // Debug logs preference implementation
    override fun saveDebugLogsEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(DEBUG_LOGS_ENABLED, enabled) }
    }

    override fun isDebugLogsEnabled(): Boolean {
        return sharedPreferences.getBoolean(DEBUG_LOGS_ENABLED, false)
    }

    // User email preference implementation
    override fun saveUserEmail(email: String?) {
        sharedPreferences.edit { putString(USER_EMAIL, email) }
    }

    override fun getUserEmail(): String? {
        return sharedPreferences.getString(USER_EMAIL, null)
    }

    // App lifecycle management for security
    override fun markAppStarted() {
        sharedPreferences.edit { 
            putBoolean(APP_STARTED, true)
            putBoolean(APP_PROPERLY_CLOSED, false)
        }
    }

    override fun markAppProperlyClosed() {
        sharedPreferences.edit { 
            putBoolean(APP_PROPERLY_CLOSED, true)
            putBoolean(APP_STARTED, false)
        }
    }

    override fun wasAppForceClosedPreviously(): Boolean {
        val wasStarted = sharedPreferences.getBoolean(APP_STARTED, false)
        val wasProperlyClosed = sharedPreferences.getBoolean(APP_PROPERLY_CLOSED, false)
        
        // If app was started but not properly closed, it was force-closed
        return wasStarted && !wasProperlyClosed
    }

    override fun clearAllOnAppClose() {
        // Mark app as properly closed before clearing
        markAppProperlyClosed()
        
        // Clear all preferences except the lifecycle flags (we need them for force-close detection)
        val editor = sharedPreferences.edit()
        
        // Preserve only the lifecycle flags
        val wasStarted = sharedPreferences.getBoolean(APP_STARTED, false)
        val wasProperlyClosed = sharedPreferences.getBoolean(APP_PROPERLY_CLOSED, false)
        
        // Clear everything
        editor.clear()
        
        // Restore lifecycle flags
        editor.putBoolean(APP_STARTED, wasStarted)
        editor.putBoolean(APP_PROPERLY_CLOSED, wasProperlyClosed)
        
        editor.apply()
        
        android.util.Log.d("PreferenceManager", "All preferences cleared on app close")
    }

    override fun clear() {
//        // Loop through all keys in the SharedPreferences
//        for (key in sharedPreferences.all.keys) {
//            // If the key is not the one to preserve, remove it
//            if (key != LOGGED_OUT) {
//                sharedPreferences.edit().remove(key)
//            }
//        }
//
//// /Commit the changes
//        sharedPreferences.edit().apply() // Use apply() for asynchronous saving

        val valueToPreserve = sharedPreferences.getBoolean(LOGGED_OUT, false)

// 2. Clear all preferences and then put the saved value back
        sharedPreferences.edit().clear().apply {
            // Only put the value back if it existed
            if (valueToPreserve != null) {
                putBoolean(LOGGED_OUT, valueToPreserve)
            }
        }.apply()
        //sharedPreferences.edit().clear().commit()
    }

    override fun saveLoggedOut(state : Boolean) {
        sharedPreferences.edit().putBoolean(LOGGED_OUT, state).commit()
    }

    override fun getIsLoggedOut(): Boolean {
        return sharedPreferences.getBoolean(LOGGED_OUT, false)
    }
}