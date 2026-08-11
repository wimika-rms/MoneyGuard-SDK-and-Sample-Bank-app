package ng.wimika.samplebankapp.network

import ng.wimika.samplebankapp.loginRepo.models.ClientLoginRequest
import ng.wimika.samplebankapp.loginRepo.models.ClientSessionResponse
import ng.wimika.samplebankapp.loginRepo.models.ShareLogsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

data class DemoStepUpRequest(val challengeReference: String, val otp: String, val purpose: String)
data class DemoStepUpResponse(
    val success: Boolean,
    val proof: String?,
    val expiresAtUtc: String?,
    val errorMessage: String?
)

interface MoneyGuardClientApiService {

    @POST("api/v1/account/auth/emails/signin")
    suspend fun login(
        @Body loginRequest: ClientLoginRequest
    ): ClientSessionResponse

    @POST("api/v1/account/auth/step-up/verify")
    suspend fun verifyStepUp(
        @Header("x-wimika-partner-token") bankSessionToken: String,
        @Body request: DemoStepUpRequest
    ): DemoStepUpResponse

    @POST("api/share-app-logs")
    suspend fun shareLogs(
        @Body shareLogsRequest: ShareLogsRequest
    ): Response<Unit>
}
