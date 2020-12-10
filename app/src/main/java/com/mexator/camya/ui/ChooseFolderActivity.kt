package com.mexator.camya.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import com.mexator.camya.databinding.ActivityChooseFolderBinding
import com.mexator.camya.extensions.getTag
import com.mexator.camya.mvvm.choose_folder.ChooseFolderViewModel
import com.mexator.camya.mvvm.choose_folder.ChooseFolderViewState
import com.mexator.camya.ui.file_list.FileAdapter
import com.mexator.camya.ui.list.StateAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class ChooseFolderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseFolderBinding
    private lateinit var viewModel: ChooseFolderViewModel

    private lateinit var viewModelSubscription: Disposable

    private val adapter = FileAdapter()
    private val stateAdapter: StateAdapter = StateAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ChooseFolderViewModel::class.java)
        binding = ActivityChooseFolderBinding.inflate(layoutInflater)

        binding.foldersList.adapter = ConcatAdapter(adapter, stateAdapter)

        viewModelSubscription = viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)

        setContentView(binding.root)

        viewModel.getFolderList("/")
    }

    private fun applyState(state: ChooseFolderViewState) {
        Log.d(getTag(), state.toString())
        with(binding) {
            if (state.loading) stateAdapter.state = StateAdapter.Loading
            adapter.submitList(state.dirList)
            adapter.notifyDataSetChanged()
        }
    }
}