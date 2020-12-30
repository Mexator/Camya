package com.mexator.camya.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mexator.camya.R
import com.mexator.camya.data.MovementDetector
import com.mexator.camya.databinding.ActivityCameraBinding
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.SingleSubject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Exception

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding

    private lateinit var cameraManager: CameraManager
    private lateinit var detector: MovementDetector
    private val recorder: MediaRecorder = MediaRecorder()
    private val surfaces: MutableList<Surface> = mutableListOf()

    companion object {
        private const val TAG = "CameraActivity"
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            // Request permissions with new API
            val launcher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { map ->
                if (map.values.any { it == false }) {
                    showMessage(R.string.error_permissions_not_granted)
                    finish()
                }
                onPermissionsGranted()
            }
            launcher.launch(REQUIRED_PERMISSIONS)
        } else
            onPermissionsGranted()
    }

    private fun onPermissionsGranted() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraID = chooseCamera()
        val a = openCamera(cameraID)
            .subscribe({ camera ->
                binding.preview.surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture?,
                            width: Int,
                            height: Int
                        ) {
                            startPreview(camera)
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture?,
                            width: Int,
                            height: Int
                        ) {
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean =
                            false

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
                    }
            }) {
                Log.e(TAG, null, it)
            }

    }

    private fun prepareRecorder() {
        recorder.reset()
        recorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            // Set output filename to recording start timestamp. They should not overlap usually
            // TODO Save to gallery?
            val format = SimpleDateFormat("dd.MM.yyyy.hh:mm:ss", Locale.US)
            val dir = filesDir.absolutePath
            val name = dir + "/" + format.format(Date())
            setOutputFile(name)
            Log.d(TAG, "saved video: $name")
            setVideoEncodingBitRate(10_000_000)
            setVideoSize(176, 144)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        }
    }

    var started = false
    private fun saveRecord() {
        if (started) {
            recorder.stop()
            started = false
        }
        recorder.reset()
    }

    private fun openCamera(cameraID: String): Single<CameraDevice> {
        val result = SingleSubject.create<CameraDevice>()
        try {
            cameraManager.openCamera(cameraID, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    result.onSuccess(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    result.onError(Exception("Opening failed"))
                }

                override fun onError(camera: CameraDevice, error: Int) {
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
            }, null)
        } catch (ex: SecurityException) {
            // This try...catch was added because of Android Studio warning
            Log.wtf(TAG, "This should never happen", ex)
        }
        return result
    }

    private fun startPreview(camera: CameraDevice) {
        val surface = Surface(binding.preview.surfaceTexture)
        // TODO Adjust input size
        detector = MovementDetector(Pair(176, 144))
        prepareRecorder()
        recorder.prepare()
        val a = detector.isDetected.subscribe({
            Log.d(TAG, "Detector:$it")
            binding.move.visibility = if (it) View.VISIBLE else View.INVISIBLE
            if (it && !started) {
                recorder.start()
                started = true
            }
        }) { it.printStackTrace() }

        val builder1 = camera
            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder1.addTarget(surface)
        builder1.addTarget(recorder.surface)
        val builder2 = camera
            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder2.addTarget(detector.surface)
        camera.createCaptureSession(
            listOf(
                surface,
                detector.surface,
                recorder.surface
            ),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    session.setRepeatingRequest(builder1.build(), null, null)
                    Observable.interval(0L, 1, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { session.capture(builder2.build(), null, null) }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}

            },
            null
        )
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

    private fun showMessage(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        saveRecord()
    }
}