package cz.synetech.app.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequestModel(
        /**
         **/
        @SerializedName("email") val email: String,
        /**
         **/
        @SerializedName("password") val password: String,
        /**
         **/
        @SerializedName("rememberMe") val rememberMe: Boolean?)
