package com.mexator.camya.mvvm.main

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mexator.camya.data.UserRepository
import com.mexator.camya.data.YandexDiskRepository
import com.mexator.camya.session.CredentialsStorage
import com.mexator.camya.util.extensions.getTag
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.regex.Pattern

class MainActivityViewModel(
    private val credentialsStorage: CredentialsStorage,
    private val userRepository: UserRepository,
    private val yandexDiskRepository: YandexDiskRepository
) : ViewModel() {
    private val _viewState: BehaviorSubject<MainActivityViewState> = BehaviorSubject.create()
    val viewState: Observable<MainActivityViewState> get() = _viewState

    init {
        _viewState.onNext(
            MainActivityViewState(
                progress = false,
                authenticated = false,
                user = null
            )
        )
    }

    private val compositeDisposable = CompositeDisposable()

    fun getAuthURL(): String = yandexDiskRepository.diskAuthURL

    /**
     * Tries to parse [uri] and get Yandex Disk Access token,
     */
    fun processUri(uri: String): Boolean {

        val matcher = Pattern
            .compile("access_token=(.*?)(&|$)")
            .matcher(uri)

        val token = if(matcher.find()) matcher.group(1) else null
        if (!token.isNullOrBlank()) {
            Log.d(getTag(), "Parsed token: $token")
            fetchUserInfo(token)
            return true
        } else {
            Log.w(getTag(), "Token not found in return url")
        }

        return false
    }

    fun tryProceedWithToken() {
        val credentials = credentialsStorage.getCachedCredentials()
        if (credentials != null) {
            fetchUserInfo(credentials.token)
        }
    }

    private fun fetchUserInfo(token: String) {
        val job = userRepository.getUser(token)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                _viewState.onNext(
                    MainActivityViewState(
                        progress = true,
                        authenticated = false,
                        user = null
                    )
                )
            }
            .subscribe(
                { user ->
                    credentialsStorage.setCredentials(user.username, token)
                    _viewState.onNext(
                        MainActivityViewState(
                            progress = false,
                            authenticated = true,
                            user = user
                        )
                    )
                },
                { error ->
                    Log.e(getTag(), "Error fetching user info", error)
                    credentialsStorage.clearToken()
                    _viewState.onNext(
                        MainActivityViewState(
                            progress = false,
                            authenticated = false,
                            user = null
                        )
                    )
                }
            )
        compositeDisposable.add(job)
    }
}