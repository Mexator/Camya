package com.mexator.camya.mvvm.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mexator.camya.data.ActualRepository
import com.mexator.camya.session.TokenStorage
import com.mexator.camya.util.extensions.getTag
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.regex.Pattern

class MainActivityViewModel(
    private val tokenStorage: TokenStorage,
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

    private val repository = ActualRepository
    private val compositeDisposable = CompositeDisposable()

    fun getAuthURL(): String = repository.diskAuthURL

    /**
     * Tries to parse [uri] and det Yandex Disk Access token,
     */
    fun processUri(uri: String): Boolean {

        val matcher = Pattern
            .compile("access_token=(.*?)(&|$)")
            .matcher(uri)

        if (matcher.find()) {
            val token = matcher.group(1)
            if (token!!.isNotBlank()) {
                Log.d(getTag(), "Parsed token: $token")
                tokenStorage.setToken(token)
                repository.setDiskToken(token)
                fetchUserInfo()
                return true
            } else {
                Log.w(getTag(), "Empty token")
            }
        } else {
            Log.w(getTag(), "Token not found in return url")
        }

        return false
    }

    fun tryProceedWithToken() {
        val token = tokenStorage.readCachedToken()
        if (token != null) {
            repository.setDiskToken(token)
            fetchUserInfo()
        }
    }

    private fun fetchUserInfo() {
        val job = repository.getUserInfo()
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
                    tokenStorage.clearToken()
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

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val applicationContext: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass == MainActivityViewModel::class.java) {
                val tokenStorage = TokenStorage(applicationContext)
                MainActivityViewModel(tokenStorage) as T
            } else {
                super.create(modelClass)
            }
        }
    }
}