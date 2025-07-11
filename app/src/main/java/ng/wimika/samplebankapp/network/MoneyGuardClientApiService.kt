package ng.wimika.samplebankapp.network

import ng.wimika.samplebankapp.loginRepo.models.ClientLoginRequest
import ng.wimika.samplebankapp.loginRepo.models.ClientSessionResponse
import ng.wimika.samplebankapp.loginRepo.models.ShareLogsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MoneyGuardClientApiService {

    @POST("api/v1/account/auth/emails/signin")
    suspend fun login(
        @Body loginRequest: ClientLoginRequest
    ): ClientSessionResponse

    @POST("api/share-app-logs")
    suspend fun shareLogs(
        @Body shareLogsRequest: ShareLogsRequest
    ): Response<Unit>
}