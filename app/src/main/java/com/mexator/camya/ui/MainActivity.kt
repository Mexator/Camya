package com.mexator.camya.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.mexator.camya.R
import com.mexator.camya.databinding.ActivityMainBinding
import com.mexator.camya.util.extensions.getTag
import com.mexator.camya.mvvm.main.MainActivityViewModel
import com.mexator.camya.mvvm.main.MainActivityViewState
import dev.androidbroadcast.vbpd.viewBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

/**
 * Main activity of an application. It can be opened at application startup and as a result of a
 * Yandex OAuth login.
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)
    private val viewModel: MainActivityViewModel by viewModels { CamyaViewModelFactory(this) }

    private lateinit var viewModelSubscription: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initEdgeToEdge()

        setOnClickListeners()
        // Subscribe to viewModel
        viewModelSubscription = viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)

        // If this activity opened as a result of intent with token, data will be not empty
        if (intent?.data != null) {
            onLogin(intent.data!!)
        } else {
            viewModel.tryProceedWithToken()
        }
    }

    private fun initEdgeToEdge() {
        enableEdgeToEdge(SystemBarStyle.dark(scrim = Color.TRANSPARENT))
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBarsInsets.bottom)
            binding.statusBarBg.updatePadding(top = systemBarsInsets.top)
            WindowInsetsCompat.CONSUMED
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