package ng.wimika.samplebankapp.loginRepo.models

import com.google.gson.annotations.SerializedName
import ng.wimika.samplebankapp.network.BaseResponse

data class ClientSession(
    @SerializedName("sessionId")
    val sessionId: String,
    @SerializedName("userFullName")
    val userFullName: String
)


class ClientSessionResponse: BaseResponse<ClientSession>()