package ng.wimika.samplebankapp.loginRepo.models
import com.google.gson.annotations.SerializedName

data class ClientLoginRequest(
    @SerializedName("Email")
    val email: String,
    @SerializedName("Password")
    val password: String
)