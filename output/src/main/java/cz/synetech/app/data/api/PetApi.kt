package cz.synetech.app.data.api

import retrofit2.Call
import retrofit2.http.*

import io.reactivex.*

import cz.synetech.app.data.generalmodels.APIResponse
import cz.synetech.app.data.generalmodels.NoData

import cz.synetech.app.data.model.CreatePetRequestModel
import cz.synetech.app.data.model.PetAPIModel

interface PetApi {
    /**
     * 
     * 
     * @return Single&lt;PetAPIModel&gt;
     */
    @GET("Pet/Get")
    fun getPetGet()
            : Single<APIResponse<PetAPIModel>>

    /**
     * 
     * 
     * @param pet
     * @return Single&lt;PetAPIModel&gt;
     */
    @POST("Pet/Create")
    fun postPetCreate(
            @Body pet: CreatePetRequestModel?)
            : Single<APIResponse<PetAPIModel>>

}
