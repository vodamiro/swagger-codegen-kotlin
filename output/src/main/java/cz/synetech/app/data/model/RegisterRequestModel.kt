package cz.synetech.app.data.model

import com.google.gson.annotations.SerializedName
import org.joda.time.DateTime

data class RegisterRequestModel(
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
        @SerializedName("firstName") val firstName: String,
        /**
         **/
        @SerializedName("lastName") val lastName: String,
        /**
         **/
        @SerializedName("nickName") val nickName: String?,
        /**
         **/
        @SerializedName("gender") val gender: String,
        /**
         **/
        @SerializedName("country") val country: String,
        /**
         **/
        @SerializedName("dateOfBirth") val dateOfBirth: DateTime,
        /**
         **/
        @SerializedName("photoUrl") val photoUrl: String?,
        /**
         **/
        @SerializedName("wallPhotoUrl") val wallPhotoUrl: String?)
