package cz.synetech.app.data.model

import com.google.gson.annotations.SerializedName

data class PetAPIModel(
        /**
         **/
        @SerializedName("guid") val guid: String,
        /**
         **/
        @SerializedName("name") val name: String,
        /**
         **/
        @SerializedName("breed") val breed: String,
        /**
         **/
        @SerializedName("behaviour") val behaviour: String?,
        /**
         **/
        @SerializedName("identNumber") val identNumber: String?)
