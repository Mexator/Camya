package com.mexator.camya.session

import android.content.Context
import androidx.core.content.edit

class TokenStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        /* name = */ "accessToken",
        /* mode = */ Context.MODE_PRIVATE
    )

    fun readCachedToken(): String? {
        return preferences.getString(TOKEN_KEY, null)
    }

    fun setToken(token: String) {
        preferences.edit {
            putString(TOKEN_KEY, token)
        }
    }

    fun clearToken() {
        preferences.edit { clear() }
    }

    private companion object {
        const val TOKEN_KEY = "token"
    }
}