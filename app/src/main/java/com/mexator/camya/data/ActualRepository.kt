package com.mexator.camya.data

import com.mexator.camya.BuildConfig
import com.mexator.camya.data.api_interface.UserAPI
import com.mexator.camya.data.model.User
import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.json.Resource
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

object ActualRepository {
    // Dependencies
    private val userApi = RetrofitUtil.getPassportRetrofit().create(UserAPI::class.java)
    private var diskClient: RestClient? = null

    private var token: String = ""

    fun setDiskToken(mToken: String) {
        token = mToken
    }

    val diskAuthURL: String =
        "https://oauth.yandex.ru/authorize?response_type=token&client_id=${BuildConfig.ID}" +
                "&force_confirm=${if (BuildConfig.DEBUG) "no" else "yes"}"


    fun getUserInfo(): Single<User> =
        userApi.getUserInfo("OAuth $token")
            .doOnSuccess { diskClient = RestClient(Credentials(it.username, token)) }

    fun getFoldersList(path: String): Single<List<Resource>> =
        Single.defer {
            Single.just(
                diskClient!!.getResources(
                    ResourcesArgs.Builder()
                        .setPath(path)
                        .setFields("_embedded")
                        .build()
                )
            )
        }
            .map { resource -> resource.resourceList.items }
            .map { list -> list.filter { item -> item.isDir } }

    fun createFolder(path: String) {
        Completable.fromRunnable { diskClient!!.makeFolder(path) }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}