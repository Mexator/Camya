package com.mexator.camya.data.api_interface

import com.mexator.camya.data.model.User
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header

interface UserAPI {

    @GET("/info/?format=json")
    fun getUserInfo(
        @Header("Authorization") token: String
    ): Single<User>
}