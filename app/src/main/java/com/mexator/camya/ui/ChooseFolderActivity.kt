package com.mexator.camya.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import com.mexator.camya.R
import com.mexator.camya.databinding.ActivityChooseFolderBinding
import com.mexator.camya.util.extensions.getTag
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
    private lateinit var state: ChooseFolderViewState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.foldersList.adapter = ConcatAdapter(adapter, stateAdapter)
        binding.buttonChooseFolder.setOnClickListener {
            adapter.chosenPosition?.let {
                val path = adapter.currentList[it]
                viewModel.folderChosen(state.currentPath + path)
                val intent = Intent(this, CameraActivity::class.java)
                startActivity(intent)
            } ?: run {
                Toast.makeText(baseContext, R.string.title_choose_folder, Toast.LENGTH_LONG).show()
            }
        }

        viewModel = ViewModelProvider(this).get(ChooseFolderViewModel::class.java)
        viewModelSubscription = viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)
        viewModel.getFolderList("/")
    }

    private fun applyState(mState: ChooseFolderViewState) {
        state = mState
        Log.d(getTag(), mState.toString())
        with(binding) {
            if (mState.loading) stateAdapter.state = StateAdapter.Loading
            else stateAdapter.state = null
            adapter.submitList(mState.dirList)
            adapter.notifyDataSetChanged()
        }
    }
}