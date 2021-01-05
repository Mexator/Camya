package com.mexator.camya.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaCodec
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
import com.mexator.camya.data.ActualRepository
import com.mexator.camya.data.MovementDetector
import com.mexator.camya.databinding.ActivityCameraBinding
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.SingleSubject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding

    private lateinit var cameraManager: CameraManager
    private lateinit var detector: MovementDetector
    private lateinit var cameraDevice: CameraDevice
    private val recorder: MediaRecorder = MediaRecorder()
    private val surfaces: MutableList<Surface> = mutableListOf()

    private val compositeDisposable = CompositeDisposable()

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

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        detector.release()
        recorder.release()
        cameraDevice.close()
        surfaces.onEach { it.release() }
    }

    private fun onPermissionsGranted() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraID = chooseCamera()
        // TODO Adjust input size
        val job = waitForPreviewSurface()
            .andThen(openCamera(cameraID))
            .subscribe({ camera ->
                cameraDevice = camera

                prepareRecorder()

                // This is very weird shit, but it is impossible to obtain surface by calling
                // recorder.getSurface() - it fails with IllegalStateException
                val recSurface = recSurf

                detector = MovementDetector(Pair(176, 144))
                val detectorSurface = detector.surface

                val previewSurface = Surface(binding.preview.surfaceTexture)
                    .also {
                        surfaces.add(it)
                    }

                startCapture(camera, previewSurface, recSurface, detectorSurface)
                startWatching()
            }) { error ->
                Log.e(TAG, null, error)
            }
        compositeDisposable.add(job)
    }

    private fun waitForPreviewSurface(): Completable {
        val result = CompletableSubject.create()
        binding.preview.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture?,
                    width: Int,
                    height: Int
                ) {
                    result.onComplete()
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
        return result
    }

    var curName = ""
    private val recSurf = MediaCodec.createPersistentInputSurface()
    private fun prepareRecorder() {
        recorder.reset()
        recorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            // Set output filename to recording start timestamp. They should not overlap usually
            val format = SimpleDateFormat("dd.MM.yyyy.HH.mm.ss", Locale.US)
            // TODO Save to gallery?
            val dir = filesDir.absolutePath
            curName = dir + "/" + format.format(Date())
            setOutputFile(curName)
            Log.d(TAG, "saved video: $curName")
            setVideoSize(176, 144)
            setVideoFrameRate(15)
            setInputSurface(recSurf)
        }
        recorder.prepare()
    }

    var started = false

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

    // Start writing to surfaces
    private fun startCapture(
        camera: CameraDevice,
        previewSurface: Surface, recorderSurface: Surface, detectorSurface: Surface
    ) {
        val previewRequest = camera
            .createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
            }.build()
        val detectorRequest = camera
            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(detector.surface)
            }.build()
        camera.createCaptureSession(
            listOf(previewSurface, detectorSurface, recorderSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    session.setRepeatingRequest(previewRequest, null, null)
                    val job =
                        Observable.interval(0L, 1, TimeUnit.SECONDS)
                            .subscribe { session.capture(detectorRequest, null, null) }
                    compositeDisposable.add(job)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            null
        )
    }

    // Start recorder when detector detects movement and stop after timeout
    private fun startWatching() {
        val job = detector.isDetected
            .distinctUntilChanged()
            .switchMapCompletable {
                binding.move.visibility = if (it) View.VISIBLE else View.INVISIBLE
                Log.d(TAG, "Detector:$it")
                if (it) {
                    if (!started) {
                        recorder.start()
                        started = true
                        Log.d(TAG, "Recording started")
                        binding.record.visibility = View.VISIBLE
                    }
                    // Do nothing
                    Completable.complete()
                } else {
                    if (started) {
                        Completable.timer(5, TimeUnit.SECONDS)
                            .doOnComplete {
                                recorder.stop()
                                ActualRepository.uploadFile(curName)
                                prepareRecorder()
                                started = false
                                Log.d(TAG, "Recording stopped")
                                binding.record.visibility = View.INVISIBLE
                            }
                    } else {
                        Completable.complete()
                    }
                }
            }
            .subscribe({}) { error -> error.printStackTrace() }
        compositeDisposable.add(job)
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
}