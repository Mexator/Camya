package com.mexator.camya.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mexator.camya.R
import com.mexator.camya.databinding.ActivityCameraBinding
import io.reactivex.Observable

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraManager: CameraManager

    companion object {
        private const val TAG = "CameraActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            // Request permissions with new API
            val launcher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                if (allPermissionsGranted()) {
                    startCamera()
                } else {
                    Toast
                        .makeText(
                            baseContext,
                            R.string.error_permissions_not_granted,
                            Toast.LENGTH_SHORT
                        )
                        .show()
                    finish()
                }
            }
            launcher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun takePhoto() {

    }


    enum class DeviceStateEvents {
        ON_OPENED, ON_CLOSED, ON_DISCONNECTED
    }

    private fun startCamera() {
        openCamera()
            .subscribe {
                Log.d(TAG, "startCamera: $it")
            }
    }

    private fun openCamera(): Observable<Pair<DeviceStateEvents, CameraDevice>> {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraID = chooseCamera()

        return Observable.create { emitter ->
            try {
                cameraManager.openCamera(cameraID, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        emitter.onNext(Pair(DeviceStateEvents.ON_OPENED, camera))
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        emitter.onNext(Pair(DeviceStateEvents.ON_DISCONNECTED, camera))
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        emitter.onError(
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

                    override fun onClosed(camera: CameraDevice) {
                        emitter.onNext(Pair(DeviceStateEvents.ON_CLOSED, camera))
                        emitter.onComplete()
                    }

                }, null)
            } catch (ex: SecurityException) {
                emitter.onError(ex)
            }
        }

    }

    private fun chooseCamera(): String {
        val cameras = cameraManager.cameraIdList
        for (camera in cameras) {
            val chars = cameraManager.getCameraCharacteristics(camera)
            chars.get(CameraCharacteristics.LENS_FACING).let { facing ->
                if (facing == CameraCharacteristics.LENS_FACING_BACK
                    || facing == CameraCharacteristics.LENS_FACING_EXTERNAL
                ) {
                    return camera
                }
            }
        }
        return cameras[0]
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }
}