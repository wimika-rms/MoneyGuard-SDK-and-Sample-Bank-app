package ng.wimika.samplebankapp

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ng.wimika.moneyguard_sdk.MoneyGuardSdk
import ng.wimika.moneyguard_sdk.services.MoneyGuardSdkService
import ng.wimika.samplebankapp.local.IPreferenceManager
import ng.wimika.samplebankapp.local.PreferenceManager
import ng.wimika.samplebankapp.ui.state.AccountProtectionFlowState

class MoneyGuardClientApp: Application(), DefaultLifecycleObserver {

    companion object {
        var sdkService: MoneyGuardSdkService? = null
        var preferenceManager: IPreferenceManager? = null
        var accountProtectionFlowState: AccountProtectionFlowState? = null
        
        // Background timeout: clear preferences after 30 minutes in background
        private const val BACKGROUND_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }

    private val appScope = CoroutineScope(Dispatchers.IO)
    private var backgroundJob: Job? = null

    override fun onCreate() {
        super<Application>.onCreate()
        preferenceManager = PreferenceManager(this)
        sdkService = MoneyGuardSdk.initialize(this)
        accountProtectionFlowState = AccountProtectionFlowState()

        // Register for process lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Check if app was force-closed previously
        checkForForceClose()
    }

    override fun onStart(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStart(owner)
        // App came to foreground
        backgroundJob?.cancel()
        backgroundJob = null
    }

    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        // App went to background - start timer to clear preferences
        backgroundJob = appScope.launch {
            delay(BACKGROUND_TIMEOUT_MS)
            // If we reach here, app has been in background for too long
            android.util.Log.d("MoneyGuardClientApp", "App in background too long - clearing preferences")
            preferenceManager?.clearAllOnAppClose()
        }
    }

    private fun checkForForceClose() {
        // If the app was previously marked as started but not properly closed,
        // it means it was force-closed
        if (preferenceManager?.wasAppForceClosedPreviously() == true) {
            android.util.Log.d("MoneyGuardClientApp", "Detected previous force-close - clearing preferences")
            preferenceManager?.clearAllOnAppClose()
        }
    }
}