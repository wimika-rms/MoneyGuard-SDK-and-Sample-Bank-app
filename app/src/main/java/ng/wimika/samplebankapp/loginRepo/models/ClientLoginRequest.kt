package ng.wimika.samplebankapp.loginRepo.models
import com.google.gson.annotations.SerializedName

data class ClientLoginRequest(
    @SerializedName("Email")
    val email: String,
    @SerializedName("Password")
    val password: String,
    @SerializedName("appVersion")
    val appVersion: String,
    @SerializedName("deviceModel")
    val deviceModel: String,
    @SerializedName("androidVersion")
    val androidVersion: String
)