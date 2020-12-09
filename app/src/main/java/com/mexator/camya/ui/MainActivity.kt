package com.mexator.camya.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mexator.camya.databinding.ActivityMainBinding
import com.mexator.camya.extensions.getTag
import com.mexator.camya.mvvm.main.MainActivityViewModel
import com.mexator.camya.mvvm.main.MainActivityViewState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

/**
 * Main activity of an application. It can be opened at application startup and as a result of a
 * Yandex OAuth login.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel

    private lateinit var viewModelSubscription: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        setOnClickListeners()

        // If this activity opened as a result of intent with token, data will be not empty
        intent?.data?.let {
            onLogin(it)
        }

        // Subscribe to viewModel
        viewModelSubscription = viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)

        setContentView(binding.root)
    }

    private fun setOnClickListeners() {
        binding.loginButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getAuthURL())))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelSubscription.dispose()
    }

    private val CHILD_LOGGED = 0
    private val CHILD_NOT_LOGGED = 1
    private fun applyState(state: MainActivityViewState) {
        Log.d(getTag(), state.toString())
        with(binding) {
            textViewUsername.text = state.user?.username
            textViewName.text = state.user?.name

            if (state.authenticated) {
                flipper.displayedChild = CHILD_LOGGED
                proceedButton.isEnabled = true
            } else {
                flipper.displayedChild = CHILD_NOT_LOGGED
                proceedButton.isEnabled = false
            }
        }
    }

    private fun onLogin(data: Uri) {
        intent = null
        viewModel.processUri(data.toString())
    }
}