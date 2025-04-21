package com.mexator.camya.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.mexator.camya.databinding.ActivityMainBinding
import com.mexator.camya.util.extensions.getTag
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
    private val viewModel: MainActivityViewModel by viewModels { CamyaViewModelFactory(this) }

    private lateinit var viewModelSubscription: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setOnClickListeners()
        // Subscribe to viewModel
        viewModelSubscription = viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)

        setContentView(binding.root)

        // If this activity opened as a result of intent with token, data will be not empty
        if (intent?.data != null) {
            onLogin(intent.data!!)
        } else {
            viewModel.tryProceedWithToken()
        }
    }

    private fun setOnClickListeners() {
        binding.loginButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getAuthURL())))
        }
        binding.proceedButton.setOnClickListener {
            val intent = Intent(this, ChooseFolderActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelSubscription.dispose()
    }

    private fun applyState(state: MainActivityViewState) {
        Log.d(getTag(), state.toString())
        with(binding) {
            textViewUsername.text = state.user?.username
            textViewName.text = state.user?.name

            if (state.authenticated) {
                flipper.displayedChild = CHILD_AUTHORIZED
                proceedButton.isEnabled = true
                Glide.with(avatar).load(state.user?.avatarUrl).into(avatar)
            } else {
                flipper.displayedChild = CHILD_UNAUTHORIZED
                proceedButton.isEnabled = false
            }
        }
    }

    private fun onLogin(data: Uri) {
        intent = null
        viewModel.processUri(data.toString())
    }

    companion object {
        private const val CHILD_AUTHORIZED = 0
        private const val CHILD_UNAUTHORIZED = 1
    }
}