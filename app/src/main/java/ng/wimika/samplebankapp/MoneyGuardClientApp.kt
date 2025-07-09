package ng.wimika.samplebankapp

import android.app.Application
import ng.wimika.moneyguard_sdk.MoneyGuardSdk
import ng.wimika.moneyguard_sdk.services.MoneyGuardSdkService
import ng.wimika.samplebankapp.local.IPreferenceManager
import ng.wimika.samplebankapp.local.PreferenceManager
import ng.wimika.samplebankapp.ui.state.AccountProtectionFlowState

class MoneyGuardClientApp: Application() {

    companion object {
        var sdkService: MoneyGuardSdkService? = null
        var preferenceManager: IPreferenceManager? = null
        var accountProtectionFlowState: AccountProtectionFlowState? = null
    }

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        sdkService = MoneyGuardSdk.initialize(this)
        accountProtectionFlowState = AccountProtectionFlowState()
    }
}