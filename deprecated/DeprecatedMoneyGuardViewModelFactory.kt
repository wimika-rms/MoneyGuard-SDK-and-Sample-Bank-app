package ng.wimika.moneyguardsdkclient.ui.features.moneyguard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ng.wimika.moneyguard_sdk.services.policy.MoneyGuardPolicy

class MoneyGuardViewModelFactory(
    private val moneyGuardPolicy: MoneyGuardPolicy,
    private val token: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoneyGuardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MoneyGuardViewModel(moneyGuardPolicy, token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 