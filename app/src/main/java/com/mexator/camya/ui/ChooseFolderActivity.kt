package com.mexator.camya.ui

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mexator.camya.databinding.ActivityChooseFolderBinding
import com.mexator.camya.mvvm.choose_folder.ChooseFolderViewModel
import com.mexator.camya.mvvm.choose_folder.ChooseFolderViewState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class ChooseFolderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseFolderBinding
    private lateinit var viewModel: ChooseFolderViewModel

    private lateinit var viewModelSubscription: Disposable

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        viewModel = ViewModelProvider(this).get(ChooseFolderViewModel::class.java)
        binding = ActivityChooseFolderBinding.inflate(layoutInflater)

        viewModelSubscription = viewModel.viewState
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)
    }

    private fun applyState(state: ChooseFolderViewState) {
        with(binding) {

        }
    }
}