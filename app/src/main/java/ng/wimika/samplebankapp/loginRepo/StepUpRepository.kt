package ng.wimika.samplebankapp.loginRepo

import ng.wimika.samplebankapp.BuildConfig
import ng.wimika.samplebankapp.network.DemoStepUpRequest
import ng.wimika.samplebankapp.network.MoneyGuardClientApiService
import ng.wimika.samplebankapp.network.NetworkUtils

class StepUpRepository {
    private val api = NetworkUtils.getRetrofitClient(BuildConfig.SABI_BANK_BASE_URL)
        .create(MoneyGuardClientApiService::class.java)

    suspend fun verify(
        bankSessionToken: String,
        challengeReference: String,
        purpose: String,
        otp: String
    ): String {
        val response = api.verifyStepUp(
            bankSessionToken,
            DemoStepUpRequest(challengeReference, otp, purpose)
        )
        return response.proof?.takeIf { response.success }
            ?: throw IllegalArgumentException(response.errorMessage ?: "OTP verification failed.")
    }
}
