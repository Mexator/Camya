package com.mexator.camya.mvvm.choose_folder

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mexator.camya.data.YandexDiskRepository
import com.mexator.camya.util.extensions.getTag
import com.yandex.disk.rest.exceptions.http.ConflictException
import com.yandex.disk.rest.json.Resource
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class ChooseFolderViewModel(private val repository: YandexDiskRepository) : ViewModel() {
    private val _viewState: BehaviorSubject<ChooseFolderViewState> = BehaviorSubject.create()
    val viewState: Observable<ChooseFolderViewState> get() = _viewState

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
        Log.d(getTag(), "Chosen: $path")
        val job = repository.createFolder("$path/Camya_records")
            .onErrorComplete {
                // dir already exists - it's okay to write there
                it is ConflictException
            }.subscribe(
                /* onComplete = */ {
                    repository.diskPath = "$path/Camya_records"
                },
                /* onError = */ { error ->
                    when (error) {
                        is YandexDiskRepository.UnauthorizedException -> Unit // todo add navigation command
                    }
                }
            )
        compositeDisposable.add(job)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}
