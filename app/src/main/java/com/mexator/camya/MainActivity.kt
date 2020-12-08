package com.mexator.camya

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    val AUTH_URL =
        "https://oauth.yandex.ru/authorize?response_type=token&client_id=" + BuildConfig.ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(AUTH_URL)
                )
            )
        }
        if (intent != null && intent.data != null) {
            onLogin()
        }
    }

    private fun onLogin() {
        val data = intent.data
        intent = null
        val pattern = Pattern.compile("access_token=(.*?)(&|$)")
        val matcher = pattern.matcher(data.toString())
        if (matcher.find()) {
            val token = matcher.group(1)
            if (!TextUtils.isEmpty(token)) {
                Log.d("TAG", "onLogin: token: $token")
                Toast.makeText(this, "Access token: $token",Toast.LENGTH_LONG).show()
            } else {
                Log.w("TAG", "onRegistrationSuccess: empty token")
            }
        } else {
            Log.w("TAG", "onRegistrationSuccess: token not found in return url")
        }
    }
}