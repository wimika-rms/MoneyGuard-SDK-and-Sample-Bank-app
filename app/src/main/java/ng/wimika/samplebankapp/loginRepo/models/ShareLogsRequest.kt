package ng.wimika.samplebankapp.loginRepo.models

import com.google.gson.annotations.SerializedName

data class ShareLogsRequest(
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("userEmail")
    val userEmail: String,
    
    @SerializedName("androidVersion")
    val androidVersion: String,
    
    @SerializedName("deviceModel")
    val deviceModel: String,
    
    @SerializedName("logContent")
    val logContent: String,
    
    @SerializedName("appVersion")
    val appVersion: String
) 