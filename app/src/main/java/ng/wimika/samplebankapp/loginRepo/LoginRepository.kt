package ng.wimika.samplebankapp.loginRepo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ng.wimika.samplebankapp.network.MoneyGuardClientApiService
import ng.wimika.samplebankapp.network.NetworkUtils
import ng.wimika.samplebankapp.loginRepo.models.ClientLoginRequest
import ng.wimika.samplebankapp.loginRepo.models.ClientSessionResponse

interface LoginRepository {
    suspend fun login(email: String, password: String, appVersion: String, deviceModel: String, androidVersion: String): Flow<ClientSessionResponse>
}


class LoginRepositoryImpl: LoginRepository {
    private val apiService: MoneyGuardClientApiService by lazy {
        NetworkUtils.getRetrofitClient("https://moneyguardrestservice-ephgezbka5ggf7cb.uksouth-01.azurewebsites.net")
            .create(MoneyGuardClientApiService::class.java)
    }

    override suspend fun login(email: String, password: String, appVersion: String, deviceModel: String, androidVersion: String): Flow<ClientSessionResponse> = flow {
        val request = ClientLoginRequest(email, password, appVersion, deviceModel, androidVersion)
        val response = apiService.login(request)
        emit(response)
    }

}