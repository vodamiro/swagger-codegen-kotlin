package cz.synetech.app.data.model

import com.google.gson.annotations.SerializedName

data class ForgotPasswordRequestModel(
        /**
         **/
        @SerializedName("email") val email: String)
