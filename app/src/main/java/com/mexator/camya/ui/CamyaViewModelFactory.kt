package com.mexator.camya.ui

import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mexator.camya.di.AppComponentHolder
import com.mexator.camya.mvvm.main.MainActivityViewModel

@Suppress("FunctionName")
fun CamyaViewModelFactory(activity: Activity): ViewModelProvider.Factory {
    return viewModelFactory {
        initializer<MainActivityViewModel> {
            val appComponent = AppComponentHolder.get(activity.applicationContext)
            MainActivityViewModel(appComponent.tokenStorage, appComponent.userRepository)
        }
    }
}