package com.mexator.camya.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
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
import io.reactivex.android.schedulers.AndroidSchedulers
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraManager: CameraManager
    lateinit var mImageReader: ImageReader
    lateinit var detector: MovementDetector

    companion object {
        private const val TAG = "CameraActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
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
                if (!map.values.all { it }) {
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
        startCamera()
    }

    private fun startCamera() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraID = chooseCamera()

        try {
            cameraManager.openCamera(cameraID, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
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
                }

                override fun onDisconnected(camera: CameraDevice) {}
                override fun onError(camera: CameraDevice, error: Int) {}
            }, null)
        } catch (ex: SecurityException) {
            // This try...catch was added because of Android Studio warning
            Log.wtf(TAG, "This should never happen", ex)
        }
    }

    private fun startPreview(camera: CameraDevice) {
        val surface = Surface(binding.preview.surfaceTexture)

        mImageReader = ImageReader.newInstance(176, 144, ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener({
            val image = mImageReader.acquireLatestImage()
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val data = ByteArray(buffer.capacity())
            buffer.get(data)
            image.close()
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            binding.iv.setImageBitmap(bitmap)
        }, null);

        detector = MovementDetector(Pair(176, 144))
        detector.isDetected.subscribe {
            Log.d(TAG, "Detector:$it")
            binding.move.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        val builder1 = camera
            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder1.addTarget(surface)
        val builder2 = camera
            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder2.addTarget(mImageReader.surface)
        builder2.addTarget(detector.surface)
        camera.createCaptureSession(
            listOf(surface, mImageReader.surface, detector.surface),
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
}