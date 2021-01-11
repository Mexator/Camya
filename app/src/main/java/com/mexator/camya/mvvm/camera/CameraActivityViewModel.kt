package com.mexator.camya.mvvm.camera

import androidx.lifecycle.ViewModel
import com.mexator.camya.data.ActualRepository
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class CameraActivityViewModel : ViewModel() {
    companion object {
        const val TAG = "CameraActivityViewModel"
    }

    private val _viewState: BehaviorSubject<CameraActivityViewState> = BehaviorSubject.create()
    val viewState: Observable<CameraActivityViewState> get() = _viewState

    init {
        _viewState.onNext(
            CameraActivityViewState()
        )
    }

    private val repository = ActualRepository


    fun recordStarted() {
        with(_viewState) {
            onNext(
                value!!.copy(isRecording = true)
            )
        }
    }

    fun recordFinished() {
        with(_viewState) {
            onNext(
                value!!.copy(isRecording = false)
            )
        }
    }

    fun setMoveDetected(detected: Boolean) {
        with(_viewState) {
            onNext(
                value!!.copy(moveDetected = detected)
            )
        }
    }

    fun uploadRecord(filePath: String) {
//        repository.uploadFile(filePath)
    }

    fun cameraError() {
        _viewState.onNext(
            CameraActivityViewState(
                moveDetected = false,
                isRecording = false,
                cameraReopenNeeded = true
            )
        )
    }

    fun cameraOpened() {
        _viewState.onNext(
            CameraActivityViewState(
                moveDetected = false,
                isRecording = false,
                cameraReopenNeeded = false
            )
        )
    }
}