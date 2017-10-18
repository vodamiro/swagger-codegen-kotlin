package cz.synetech.app.data.model

import com.google.gson.annotations.SerializedName

data class ExternalLoginRequestModel(
        /**
         **/
        @SerializedName("email") val email: String)
