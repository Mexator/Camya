package com.mexator.camya.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mexator.camya.databinding.ActivityMainBinding
import com.mexator.camya.extensions.getTag
import com.mexator.camya.mvvm.main.MainActivityViewModel
import com.mexator.camya.mvvm.main.MainActivityViewState
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        val button = binding.button
        button.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getAuthURL())))
        }

        intent?.data?.let {
            onLogin(it)
        }

        setContentView(binding.root)

        viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)
    }

    private fun applyState(state: MainActivityViewState) {
        binding.textViewUsername.text = state.user?.username
        binding.textViewName.text = state.user?.name
        Log.d(getTag(),state.toString())
    }

    private fun onLogin(data: Uri) {
        intent = null
        viewModel.processUri(data.toString())
    }
}