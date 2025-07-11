package ng.wimika.samplebankapp.loginRepo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ng.wimika.samplebankapp.network.MoneyGuardClientApiService
import ng.wimika.samplebankapp.network.NetworkUtils
import ng.wimika.samplebankapp.loginRepo.models.ShareLogsRequest
import retrofit2.Response

interface ShareLogsRepository {
    suspend fun shareLogs(
        userEmail: String,
        androidVersion: String,
        deviceModel: String,
        logContent: String,
        appVersion: String
    ): Flow<Boolean>
}

class ShareLogsRepositoryImpl: ShareLogsRepository {

    private val apiService: MoneyGuardClientApiService by lazy {
        NetworkUtils.getRetrofitClient("https://moneyguardrestservice-ephgezbka5ggf7cb.uksouth-01.azurewebsites.net")
            .create(MoneyGuardClientApiService::class.java)
    }

    override suspend fun shareLogs(
        userEmail: String,
        androidVersion: String,
        deviceModel: String,
        logContent: String,
        appVersion: String
    ): Flow<Boolean> = flow {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val request = ShareLogsRequest(
            timestamp = timestamp,
            userEmail = userEmail,
            androidVersion = androidVersion,
            deviceModel = deviceModel,
            logContent = logContent,
            appVersion = appVersion
        )
        val response = apiService.shareLogs(request)
        emit(response.isSuccessful)
    }
} 