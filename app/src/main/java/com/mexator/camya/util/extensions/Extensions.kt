package com.mexator.camya.util.extensions

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresPermission
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

fun Any.getTag() = this.javaClass.simpleName

fun Size.toPair() = Pair(this.height, this.width)

@RequiresPermission(android.Manifest.permission.CAMERA)
fun CameraManager.openCameraRx(cameraID: String, handler: Handler): Observable<CameraDevice> {
    Log.d("CameraManager", "openCamera")
    val result = BehaviorSubject.create<CameraDevice>()
    try {
        openCamera(cameraID, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                result.onNext(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                try {
                    camera.close()
                } catch (ex: Exception) {
                }
                result.onError(Throwable("CameraDevice disconnected"))
            }

            override fun onError(camera: CameraDevice, error: Int) {
                try {
                    camera.close()
                } catch (ex: Exception) {
                }
                result.onError(
                    when (error) {
                        ERROR_CAMERA_IN_USE -> Throwable("ERROR_CAMERA_IN_USE")
                        ERROR_MAX_CAMERAS_IN_USE -> Throwable("ERROR_MAX_CAMERAS_IN_USE")
                        ERROR_CAMERA_DISABLED -> Throwable("ERROR_CAMERA_DISABLED")
                        ERROR_CAMERA_DEVICE -> Throwable("ERROR_CAMERA_DEVICE")
                        ERROR_CAMERA_SERVICE -> Throwable("ERROR_CAMERA_SERVICE")
                        else -> Throwable("Some other error, code $error")
                    }
                )
            }
        }, handler)
    } catch (ex: Exception) {
        result.onError(ex)
    }
    return result
}