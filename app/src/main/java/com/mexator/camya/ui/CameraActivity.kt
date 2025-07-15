package com.mexator.camya.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.mexator.camya.R
import com.mexator.camya.data.MovementDetector
import com.mexator.camya.databinding.ActivityCameraBinding
import com.mexator.camya.mvvm.camera.CameraActivityViewModel
import com.mexator.camya.mvvm.camera.CameraActivityViewState
import com.mexator.camya.util.extensions.openCameraRx
import com.mexator.camya.util.functions.getSmallestResolution
import dev.androidbroadcast.vbpd.viewBinding
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.CompletableSubject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Most of the functions in this Activity are just Rx wrappers for callbacks
class CameraActivity : AppCompatActivity(R.layout.activity_camera) {
    private val binding: ActivityCameraBinding by viewBinding(ActivityCameraBinding::bind)
    private val viewModel: CameraActivityViewModel by viewModels()

    private val cameraManager: CameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    private lateinit var detector: MovementDetector

    private var recorder: MediaRecorder = MediaRecorder()
    private val recorderSurface = MediaCodec.createPersistentInputSurface()


    private val surfaces: MutableList<Surface> = mutableListOf()
    private val viewModelDisposable = CompositeDisposable()
    private val compositeDisposable = CompositeDisposable()

    private var state = InternalState()

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUIRED_PERMISSIONS = Manifest.permission.CAMERA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initEdgeToEdge()

        // Request permissions with new API
        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(this, R.string.error_permissions_not_granted, Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
        launcher.launch(REQUIRED_PERMISSIONS)
    }

    private fun initEdgeToEdge() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.overlay.updatePadding(
                bottom = systemBarsInsets.bottom,
                top = systemBarsInsets.top
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onStart() {
        super.onStart()
        viewModelDisposable.clear()
        val job = viewModel.viewState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                applyViewState(it)
            }
        viewModelDisposable.add(job)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
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
            binding.record.visibility = if (isRecording) View.VISIBLE else View.GONE
            binding.move.visibility = if (moveDetected) View.VISIBLE else View.GONE
            if (cameraReopenNeeded) {
                compositeDisposable.clear()
                recorder.reset()
                surfaces.onEach { it.release() }
                recorder = MediaRecorder()
                startCamera()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCamera() {
        val cameraID = chooseCamera()

        val job = waitForPreviewSurface()
            .andThen(cameraManager.openCameraRx(cameraID, cameraHandler))
            .subscribeOn(AndroidSchedulers.from(cameraHandler.looper))
            .subscribe({ camera ->

                viewModel.cameraOpened()
                prepareRecorder()

                detector =
                    MovementDetector(getSmallestResolution(state.chosenCameraChars!!))
                val detectorSurface = detector.surface

                val previewSurface = binding.preview.holder.surface
                    .also {
                        surfaces.add(it)
                    }

                startCapture(camera, previewSurface, recorderSurface, detectorSurface)
                startWatching()
            }) { error ->
                Log.e(TAG, null, error)
                viewModel.cameraError()
            }
        compositeDisposable.add(job)
    }

    private fun waitForPreviewSurface(): Completable {
        val result = CompletableSubject.create()
        if (!binding.preview.holder.isCreating) {
            result.onComplete()
        } else {
            binding.preview.holder.addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        result.onComplete()
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {}
                }
            )
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
            setInputSurface(recorderSurface)
        }
        recorder.prepare()
        state = state.copy(curPath = curName)
    }

    // Start writing to surfaces
    private fun startCapture(
        camera: CameraDevice,
        previewSurface: Surface, recorderSurface: Surface, detectorSurface: Surface
    ) {
        // Here I could use two separate requests, one for recorder and preview, and the other
        // for detector, invoked once per second, but I have one device that is unable to capture
        // second request while processing repeating request. That is why I designed MovementDetector
        // in such way

        val previewRequest = camera
            .createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
                addTarget(detectorSurface)
            }.build()

        camera.createCaptureSession(
            listOf(previewSurface, detectorSurface, recorderSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    session.setRepeatingRequest(previewRequest, null, cameraHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            cameraHandler
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

    private data class InternalState(
        val curPath: String? = null,
        val chosenCameraChars: CameraCharacteristics? = null,
        val started: Boolean = false
    )
}