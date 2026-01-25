package com.mexator.camya.session

import android.content.Context
import androidx.core.content.edit

class CredentialsStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        /* name = */ "credentials",
        /* mode = */ Context.MODE_PRIVATE
    )

    fun getCachedCredentials(): Credentials? {
        val username = preferences.getString(USERNAME_KEY, null)
        val token = preferences.getString(TOKEN_KEY, null)
        if (username == null || token == null) return null
        return Credentials(
            username = username,
            token = token,
        )
    }

    fun setCredentials(username: String, token: String) {
        preferences.edit {
            putString(USERNAME_KEY, username)
            putString(TOKEN_KEY, token)
        }
    }

    fun clearToken() {
        preferences.edit { clear() }
    }

    private companion object {
        const val USERNAME_KEY = "user"
        const val TOKEN_KEY = "token"
    }
}