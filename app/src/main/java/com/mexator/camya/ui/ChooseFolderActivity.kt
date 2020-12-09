package com.mexator.camya.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mexator.camya.databinding.ActivityChooseFolderBinding
import com.mexator.camya.mvvm.choose_folder.ChooseFolderViewModel
import com.mexator.camya.mvvm.choose_folder.ChooseFolderViewState
import com.mexator.camya.ui.list.StateAdapterUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class ChooseFolderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseFolderBinding
    private lateinit var viewModel: ChooseFolderViewModel

    private lateinit var viewModelSubscription: Disposable

    private val stateAdapterUtil: StateAdapterUtil = StateAdapterUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ChooseFolderViewModel::class.java)
        binding = ActivityChooseFolderBinding.inflate(layoutInflater)

        binding.foldersList.adapter = stateAdapterUtil.getAdapter()

        viewModelSubscription = viewModel.viewState
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)

        setContentView(binding.root)
    }

    private fun applyState(state: ChooseFolderViewState) {
        with(binding) {
            if (state.loading) stateAdapterUtil.state = StateAdapterUtil.Loading
            else stateAdapterUtil.state = StateAdapterUtil.Error("error")
        }
    }
}