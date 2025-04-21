package com.mexator.camya.di

import android.content.Context
import com.mexator.camya.data.UserRepository
import com.mexator.camya.data.userinfo.api.UserAPI
import com.mexator.camya.session.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object AppComponentHolder {

    private var cachedComponent: AppComponent? = null

    fun get(applicationContext: Context): AppComponent {
        return cachedComponent ?: AppModule(applicationContext)
    }
}

interface AppComponent {

    val userRepository: UserRepository
    val tokenStorage: TokenStorage
}

fun AppModule(applicationContext: Context): AppComponent {

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
        ).build()
    val passportRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://login.yandex.ru")
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val userApi = passportRetrofit.create(UserAPI::class.java)

    return object : AppComponent {
        override val userRepository: UserRepository = UserRepository(userApi)
        override val tokenStorage: TokenStorage = TokenStorage(applicationContext)
    }
}