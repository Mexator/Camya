package com.mexator.camya.mvvm.choose_folder

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mexator.camya.data.ActualRepository
import com.mexator.camya.extensions.getTag
import com.yandex.disk.rest.json.Resource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class ChooseFolderViewModel : ViewModel() {
    private val _viewState: BehaviorSubject<ChooseFolderViewState> = BehaviorSubject.create()
    val viewState: Observable<ChooseFolderViewState> get() = _viewState

    private val repository = ActualRepository

    private val compositeDisposable = CompositeDisposable()

    init {
        _viewState.onNext(
            ChooseFolderViewState(
                loading = false,
                dirList = emptyList(),
                currentPath = "/",
                chosenItem = null
            )
        )
    }

    fun getFolderList(path: String) {
        val job = repository.getFoldersList(path)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                _viewState.onNext(
                    _viewState.value!!.copy(
                        loading = true
                    )
                )
            }
            .subscribe { list: List<Resource> ->
                _viewState.onNext(
                    _viewState.value!!.copy(
                        loading = false,
                        dirList = list.map { it.name }
                    )
                )
            }
        compositeDisposable.add(job)
    }

    fun folderChosen(path: String) {
        Log.d(getTag(),"Chosen: $path")
        repository.createFolder("$path/Camya_records")
        repository.diskPath = "$path/Camya_records"
    }
}