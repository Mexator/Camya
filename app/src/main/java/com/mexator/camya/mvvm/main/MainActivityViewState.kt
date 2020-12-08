package com.mexator.camya.mvvm.main

import com.mexator.camya.data.model.User

data class MainActivityViewState(
    val progress: Boolean,
    val authenticated: Boolean,
    val user: User?
)