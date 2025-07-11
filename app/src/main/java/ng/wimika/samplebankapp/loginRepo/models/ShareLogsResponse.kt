package ng.wimika.samplebankapp.loginRepo.models

import com.google.gson.annotations.SerializedName

data class ShareLogsResponse(
    @SerializedName("isError")
    val isError: Boolean,
    
    @SerializedName("errorMessage")
    val errorMessage: String?,
    
    @SerializedName("data")
    val data: String?
) 