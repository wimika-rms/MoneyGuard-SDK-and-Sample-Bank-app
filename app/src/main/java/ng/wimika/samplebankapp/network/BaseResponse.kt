package ng.wimika.samplebankapp.network

import com.google.gson.annotations.SerializedName

open class BaseResponse <T> {
    @SerializedName("isError")
    var isError: Boolean = false

    @SerializedName("errorMessage")
    var errorMessage: String? = null

    @SerializedName("data")
    var data: T? = null
}
