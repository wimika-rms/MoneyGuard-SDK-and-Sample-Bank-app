package ng.wimika.samplebankapp.ui.screens.claims

import android.net.Uri
import ng.wimika.moneyguard_sdk.services.claims.datasource.model.ClaimResponse
import ng.wimika.moneyguard_sdk.services.claims.datasource.model.ClaimStatus
import ng.wimika.moneyguard_sdk.services.moneyguard_policy.models.BankAccount
import java.util.Date


// State for SubmitClaimScreen
data class SubmitClaimState(
    val accounts: List<BankAccount> = emptyList(),
    val selectedAccount: BankAccount? = null,
    val incidentNames: List<String> = emptyList(),
    val nameofIncident: String? = null,
    val lossAmount: Double = 0.0,
    val lossDate: Date? = null,
    val statement: String = "",
    val selectedFiles: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val showDatePicker: Boolean = false,
    val showPermissionRationale: Boolean = false
) {
    val shouldEnableButton: Boolean
        get() = selectedAccount != null &&
                nameofIncident != null &&
                lossAmount > 0 &&
                lossDate != null &&
                statement.isNotBlank() &&
                !isLoading
}

// Events for SubmitClaimScreen
sealed class SubmitClaimEvent {
    data class AccountSelected(val account: BankAccount) : SubmitClaimEvent()
    data class NameOfIncidentChanged(val incidentName: String) : SubmitClaimEvent()
    data class LossAmountChanged(val amount: Double) : SubmitClaimEvent()
    data class LossDateChanged(val date: Date) : SubmitClaimEvent()
    data class StatementChanged(val statement: String) : SubmitClaimEvent()
    data class OnFilesSelected(val uris: List<Uri>) : SubmitClaimEvent()
    object ShowDatePicker : SubmitClaimEvent()
    object HideDatePicker : SubmitClaimEvent()
    object ShowPermissionRationale : SubmitClaimEvent()
    object HidePermissionRationale : SubmitClaimEvent()
    object SubmitClaim : SubmitClaimEvent()
}

// State for ClaimsListScreen
data class ClaimsListState(
    val claims: List<ClaimResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val status: ClaimStatus? = null // Filter by status
)

// Events for ClaimsListScreen
sealed class ClaimsListEvent {
    object LoadClaims : ClaimsListEvent()
    object RefreshClaims : ClaimsListEvent()
    data class FilterByStatus(val status: ClaimStatus?) : ClaimsListEvent()
}

// State for ClaimsDetailsScreen  
data class ClaimsDetailsState(
    val claim: ClaimResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) 