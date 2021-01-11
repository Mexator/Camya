package com.mexator.camya.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mexator.camya.R
import com.mexator.camya.data.MovementDetector
import com.mexator.camya.databinding.ActivityCameraBinding
import com.mexator.camya.extensions.toPair
import com.mexator.camya.mvvm.camera.CameraActivityViewModel
import com.mexator.camya.mvvm.camera.CameraActivityViewState
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.CompletableSubject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private val viewModel: CameraActivityViewModel by viewModels()

    private lateinit var cameraManager: CameraManager
    private lateinit var detector: MovementDetector

    private var recorder: MediaRecorder = MediaRecorder()
    private val recSurf = MediaCodec.createPersistentInputSurface()


    private val surfaces: MutableList<Surface> = mutableListOf()
    private val viewModelDisposable = CompositeDisposable()
    private val compositeDisposable = CompositeDisposable()

    private var state = InternalState()

    companion object {
        private const val TAG = "CameraActivity"
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
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
        viewModelDisposable.dispose()
        compositeDisposable.dispose()
        detector.release()
        recorder.release()
        surfaces.onEach { it.release() }
        state.curPath?.let {
            File(it).delete()
        }
    }

    @Synchronized
    private fun applyViewState(state: CameraActivityViewState) {
        Log.d(TAG, state.toString())
        with(state) {
            binding.move.visibility = if (moveDetected) View.VISIBLE else View.INVISIBLE
            binding.record.visibility = if (isRecording) View.VISIBLE else View.INVISIBLE
            if (cameraReopenNeeded) {
                compositeDisposable.clear()
                recorder.reset()
                surfaces.onEach { it.release() }
                recorder = MediaRecorder()
                startCamera()
            }
        }
    }

    private fun onPermissionsGranted() {
        viewModelDisposable.clear()
        val job = viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                applyViewState(it)
            }
        viewModelDisposable.add(job)
    }

    private fun startCamera() {
        val cameraID = chooseCamera()

        val job = waitForPreviewSurface()
            .andThen(openCamera(cameraID))
            .subscribe({ camera ->

                viewModel.cameraOpened()
                prepareRecorder()

                // This is very weird shit, but it is impossible to obtain surface by calling
                // recorder.getSurface() - it fails with IllegalStateException
                val recSurface = recSurf

                detector =
                    MovementDetector(getSmallestResolution(state.chosenCameraChars!!).toPair())
                val detectorSurface = detector.surface

                val previewSurface = Surface(binding.preview.surfaceTexture)
                    .also {
                        surfaces.add(it)
                    }

                startCapture(camera, previewSurface, recSurface, detectorSurface)
                startWatching()
            }) { error ->
                Log.e(TAG, null, error)
                viewModel.cameraError()
            }
        compositeDisposable.add(job)
    }

    private fun waitForPreviewSurface(): Completable {
        val result = CompletableSubject.create()
        if (binding.preview.isAvailable) {
            result.onComplete()
        } else {
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
                        true

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
                }
        }
        return result
    }

    private fun prepareRecorder() {
        recorder.reset()

        // Set output filename to recording start timestamp. They should not overlap usually
        val format = SimpleDateFormat("dd.MM.yyyy.HH.mm.ss", Locale.US)
        // TODO Save to gallery?
        val curName = filesDir.absolutePath + "/" + format.format(Date())

        recorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOutputFile(curName)
            Log.d(TAG, curName)
            val size = getSmallestResolution(state.chosenCameraChars!!)
            setVideoSize(size.width, size.height)
            setVideoFrameRate(15)
            setInputSurface(recSurf)
        }
        recorder.prepare()
        state = state.copy(curPath = curName)
    }

    private fun openCamera(cameraID: String): Observable<CameraDevice> {
        Log.d(TAG, "openCamera")
        val result = BehaviorSubject.create<CameraDevice>()
        try {
            cameraManager.openCamera(cameraID, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    result.onNext(camera)
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
        } catch (ex: Exception) {
            viewModel.cameraError()
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
                addTarget(detectorSurface)
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

    // Start recorder when detector detects movement and stop after timeout. Cancel timeout,
    // if move is detected
    private fun startWatching() {
        val job = detector.isDetected
            .distinctUntilChanged()
            .switchMapCompletable {
                Log.d(TAG, "Detector:$it")
                viewModel.setMoveDetected(it)
                if (it) {
                    onMove()
                    Completable.complete()
                } else {
                    if (state.started) {
                        Completable.timer(5, TimeUnit.SECONDS)
                            .doOnComplete {
                                softStop()
                            }
                    } else {
                        Completable.complete()
                    }
                }
            }
            .subscribe({}) { error -> error.printStackTrace() }
        compositeDisposable.add(job)
    }

    @Synchronized
    private fun onMove() {
        if (!state.started) {
            try {
                recorder.start()
                state = state.copy(started = true)
                Log.d(TAG, "Recording started")
                viewModel.recordStarted()
            } catch (ex: Exception) {
                Log.e(TAG, "Can't start MediaRecorder:", ex)
            }
        }
    }

    // Try to stop recorder and upload file
    @Synchronized
    private fun softStop() {
        try {
            recorder.stop()
            state = state.copy(started = false)
            viewModel.recordFinished()
            state.curPath?.let {
                viewModel.uploadRecord(it)
            }
            Log.d(TAG, "Recording stopped")
            prepareRecorder()
        } catch (ex: Exception) {
            Log.e(TAG, "Can't stop MediaRecorder:", ex)
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
                    state = state.copy(chosenCameraChars = chars)
                    return camera
                }
            }
        }
        val chars = cameraManager.getCameraCharacteristics(cameras[0])
        state = state.copy(chosenCameraChars = chars)
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

    private fun getSmallestResolution(characteristics: CameraCharacteristics): Size {
        val confMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = confMap!!.getOutputSizes(ImageFormat.JPEG)
        // Usually the cameras keep aspect ratio, so I can compare just heights
        var minH = -1
        var index = 0
        for ((idx, value) in sizes.withIndex()) {
            if (minH > value.height || minH == -1) {
                index = idx
                minH = value.height
            }
        }
        return sizes[index]
    }

    private data class InternalState(
        val curPath: String? = null,
        val chosenCameraChars: CameraCharacteristics? = null,
        val started: Boolean = false
    )
}