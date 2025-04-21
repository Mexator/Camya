package com.mexator.camya.data.userinfo.api

import com.mexator.camya.data.userinfo.api.dto.UserDto
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * https://yandex.ru/dev/id/doc/ru/user-information
 */
interface UserAPI {

    @GET("/info/?format=json")
    fun getUserInfo(
        @Header("Authorization") token: String
    ): Single<UserDto>
}