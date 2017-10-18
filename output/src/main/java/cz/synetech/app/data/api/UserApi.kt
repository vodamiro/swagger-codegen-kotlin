package cz.synetech.app.data.api

import retrofit2.Call
import retrofit2.http.*

import io.reactivex.*

import cz.synetech.app.data.generalmodels.APIResponse
import cz.synetech.app.data.generalmodels.NoData

import cz.synetech.app.data.model.ExternalLoginRequestModel
import cz.synetech.app.data.model.ForgotPasswordRequestModel
import cz.synetech.app.data.model.LoginRequestModel
import cz.synetech.app.data.model.RegisterRequestModel
import cz.synetech.app.data.model.ResetPasswordRequestModel
import cz.synetech.app.data.model.UserAPIModel

interface UserApi {
    /**
     * 
     * 
     * @param provider
     * @return Single&lt;UserAPIModel&gt;
     */
    @POST("User/ExternalLogin")
    fun postUserExternalLogin(
            @Query("provider") provider: String?)
            : Single<APIResponse<UserAPIModel>>

    /**
     * 
     * 
     * @param model
     * @return Single<APIResponse<EmptyResponse>>
     */
    @POST("User/ExternalLoginConfirmation")
    fun postUserExternalLoginConfirmation(
            @Body model: ExternalLoginRequestModel?)
            : Single<APIResponse<EmptyResponse>>

    /**
     * 
     * 
     * @param model
     * @return Single<APIResponse<EmptyResponse>>
     */
    @POST("User/ForgotPassword")
    fun postUserForgotPassword(
            @Body model: ForgotPasswordRequestModel?)
            : Single<APIResponse<EmptyResponse>>

    /**
     * 
     * 
     * @param model
     * @return Single&lt;UserAPIModel&gt;
     */
    @POST("User/Login")
    fun postUserLogin(
            @Body model: LoginRequestModel?)
            : Single<APIResponse<UserAPIModel>>

    /**
     * 
     * 
     * @return Single<APIResponse<EmptyResponse>>
     */
    @POST("User/Logout")
    fun postUserLogout()
            : Single<APIResponse<EmptyResponse>>

    /**
     * 
     * 
     * @param model
     * @return Single&lt;UserAPIModel&gt;
     */
    @POST("User/Register")
    fun postUserRegister(
            @Body model: RegisterRequestModel?)
            : Single<APIResponse<UserAPIModel>>

    /**
     * 
     * 
     * @param model
     * @return Single<APIResponse<EmptyResponse>>
     */
    @POST("User/ResetPassword")
    fun postUserResetPassword(
            @Body model: ResetPasswordRequestModel?)
            : Single<APIResponse<EmptyResponse>>

}
