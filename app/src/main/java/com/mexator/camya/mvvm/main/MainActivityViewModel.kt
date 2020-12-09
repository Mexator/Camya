package com.mexator.camya.mvvm.main

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mexator.camya.data.ActualRepository
import com.mexator.camya.extensions.getTag
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.regex.Pattern

class MainActivityViewModel : ViewModel() {
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
                repository.setDiskToken(token)
                getInfo()
                return true
            } else {
                Log.w(getTag(), "Empty token")
            }
        } else {
            Log.w(getTag(), "Token not found in return url")
        }

        return false
    }

    private fun getInfo() {
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
            .subscribe { user ->
                _viewState.onNext(
                    MainActivityViewState(
                        progress = false,
                        authenticated = true,
                        user = user
                    )
                )
            }
        compositeDisposable.add(job)
    }
}