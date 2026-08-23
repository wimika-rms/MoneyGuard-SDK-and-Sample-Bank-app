package ng.wimika.samplebankapp.ui.state

const val MONEYGUARD_TEMPORARILY_UNAVAILABLE_MESSAGE =
    "MoneyGuard protection is temporarily unavailable. Your bank login will continue."

fun moneyGuardSecurityWarning(failureCode: String?): String = when (failureCode) {
    "caller_not_authorised" ->
        "MoneyGuard could not verify this bank integration. Your bank login will continue without MoneyGuard protection."
    "host_attestation_binding_failed", "host_attestation_failed" ->
        "MoneyGuard protection could not be verified on this device. Your bank login will continue without MoneyGuard protection."
    else ->
        "MoneyGuard protection could not be verified. Your bank login will continue without MoneyGuard protection."
}
