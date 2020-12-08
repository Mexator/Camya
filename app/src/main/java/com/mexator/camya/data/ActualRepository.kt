package com.mexator.camya.data

import com.mexator.camya.BuildConfig
import com.mexator.camya.data.api_interface.UserAPI
import com.mexator.camya.data.model.User
import io.reactivex.Single

object ActualRepository {
    // Dependencies
    private val userApi = RetrofitUtil.getPassportRetrofit().create(UserAPI::class.java)

    private var token: String = ""


    fun setDiskToken(mToken: String) {
        token = mToken
    }

    val diskAuthURL: String =
        "https://oauth.yandex.ru/authorize?response_type=token&client_id=${BuildConfig.ID}&force_confirm=yes"

    fun getUserInfo(): Single<User> =
        userApi.getUserInfo("OAuth $token")

}