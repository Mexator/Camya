package com.mexator.camya.mvvm.choose_folder

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class ChooseFolderViewModel : ViewModel() {
    private val _viewState: BehaviorSubject<ChooseFolderViewState> = BehaviorSubject.create()
    val viewState: Observable<ChooseFolderViewState> get() = _viewState

    init {
        _viewState.onNext(
            ChooseFolderViewState(
                loading = false,
                dirList = emptyList(),
                currentPath = "",
                chosenItem = null
            )
        )
    }


}