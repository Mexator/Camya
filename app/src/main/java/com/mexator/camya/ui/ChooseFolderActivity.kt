package com.mexator.camya.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import com.mexator.camya.R
import com.mexator.camya.databinding.ActivityChooseFolderBinding
import com.mexator.camya.util.extensions.getTag
import com.mexator.camya.mvvm.choose_folder.ChooseFolderViewModel
import com.mexator.camya.mvvm.choose_folder.ChooseFolderViewState
import com.mexator.camya.ui.file_list.FileAdapter
import com.mexator.camya.ui.list.StateAdapter
import dev.androidbroadcast.vbpd.viewBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class ChooseFolderActivity : AppCompatActivity(R.layout.activity_choose_folder) {
    private val binding: ActivityChooseFolderBinding by viewBinding(ActivityChooseFolderBinding::bind)
    private val viewModel: ChooseFolderViewModel by viewModels()

    private lateinit var viewModelSubscription: Disposable

    private val adapter = FileAdapter()
    private val stateAdapter: StateAdapter = StateAdapter()
    private lateinit var state: ChooseFolderViewState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initEdgeToEdge()

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

        viewModelSubscription = viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::applyState)
        viewModel.getFolderList("/")
    }

    private fun initEdgeToEdge() {
        enableEdgeToEdge(SystemBarStyle.dark(scrim = Color.TRANSPARENT))
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.statusBarBg.updatePadding(top = systemBarsInsets.top)
            binding.floatingButtons.updatePadding(
                bottom = systemBarsInsets.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun applyState(mState: ChooseFolderViewState) {
        state = mState
        Log.d(getTag(), mState.toString())
        if (mState.loading) stateAdapter.state = StateAdapter.Loading
        else stateAdapter.state = null
        adapter.submitList(mState.dirList)
        adapter.notifyDataSetChanged()
    }
}