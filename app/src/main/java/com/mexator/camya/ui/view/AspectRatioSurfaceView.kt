package com.mexator.camya.ui.view

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.AttributeSet
import android.view.SurfaceView
import kotlin.math.min

class AspectRatioSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {
    private var previewWidth: Float = 0f
    private var previewHeight: Float = 0f
    private var char: CameraCharacteristics? = null

    fun setPreviewSize(width: Int, height: Int, rot: CameraCharacteristics) {
        previewWidth = width.toFloat()
        previewHeight = height.toFloat()
        char = rot
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)


        if (previewWidth > 0f && previewHeight > 0f) {
            val relativeRotation = computeRelativeRotation(char!!, 0)
            /* Scale factor required to scale the preview to its original size on the x-axis. */
            val scaleX =
                if (relativeRotation % 180 == 0) {
                    width.toFloat() / previewWidth
                } else {
                    width.toFloat() / previewHeight
                }
            /* Scale factor required to scale the preview to its original size on the y-axis. */
            val scaleY =
                if (relativeRotation % 180 == 0) {
                    height.toFloat() / previewHeight
                } else {
                    height.toFloat() / previewWidth
                }

            /* Scale factor required to fit the preview to the SurfaceView size. */
            val finalScale = min(scaleX, scaleY)

            setScaleX(1 / scaleX * finalScale)
            setScaleY(1 / scaleY * finalScale)
        }
        setMeasuredDimension(width, height)
    }

    fun computeRelativeRotation(
        characteristics: CameraCharacteristics,
        surfaceRotationDegrees: Int
    ): Int {
        val sensorOrientationDegrees =
            characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Reverse device orientation for back-facing cameras.
        val sign = if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
            CameraCharacteristics.LENS_FACING_FRONT
        ) 1 else -1

        // Calculate desired orientation relative to camera orientation to make
        // the image upright relative to the device orientation.
        return (sensorOrientationDegrees - surfaceRotationDegrees * sign + 360) % 360

    }
}