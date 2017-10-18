package cz.synetech.app.data.model

import com.google.gson.annotations.SerializedName

data class ResetPasswordRequestModel(
        /**
         **/
        @SerializedName("email") val email: String,
        /**
         **/
        @SerializedName("password") val password: String,
        /**
         **/
        @SerializedName("confirmPassword") val confirmPassword: String?,
        /**
         **/
        @SerializedName("code") val code: String?)
