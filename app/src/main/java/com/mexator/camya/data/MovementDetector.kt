package com.mexator.camya.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.util.Log
import android.view.Surface
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * <p> This class implements movement detection functionality. It has [surface] on which
 * producers (usually camera) can draw on, and also [isDetected] that emits whether
 * movement detected or not. </p>
 * @property surface surface on which producers can draw. When image is drawn, it is
 * compared with previous image drawn on the surface. First image drawn on empty surface
 * does not considered as movement.
 * @property isDetected observable that emits boolean values on each incoming picture.
 * if movement detected, **true** emitted, otherwise false
 */
class MovementDetector(private val inputSize: Pair<Int, Int>) {
    companion object {
        private const val TAG = "MovementDetector"
        private const val THRESHOLD = 10
    }

    val surface: Surface
    val isDetected: Observable<Boolean>

    private var prevImage: Bitmap? = null
        set(value) {
            field?.recycle()
            field = value
        }
    private val imageReader: ImageReader = ImageReader
        .newInstance(
            inputSize.first,
            inputSize.second,
            ImageFormat.JPEG,
            1
        )
    private val listener = ImageReader.OnImageAvailableListener {
        val image = imageReader.acquireNextImage()
        // Decode as Bitmap
        val planes: Array<Image.Plane> = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val data = ByteArray(buffer.capacity())
        buffer.get(data)
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        image.close()

        // To make sure that prevImage won't be null in if body
        val mPrevImage = prevImage
        if (mPrevImage == null) {
            _isDetected.onNext(false)
        } else {
            val difference = pixelDiffPercent(mPrevImage, bitmap)
            _isDetected.onNext(difference > THRESHOLD)
        }
        prevImage = bitmap
    }

    init {
        imageReader.setOnImageAvailableListener(listener, null)
        surface = imageReader.surface
    }

    private val _isDetected: PublishSubject<Boolean> = PublishSubject.create()

    init {
        isDetected = _isDetected
    }

    fun release() {
        imageReader.close()
        prevImage?.recycle()
    }

    private fun pixelDiffPercent(image1: Bitmap, image2: Bitmap): Double {
        if (image1.width != image2.width || image1.height != image2.height) {
            val f = "(%d,%d) vs. (%d,%d)".format(
                image1.width,
                image1.height,
                image2.width,
                image2.height
            )
            throw IllegalArgumentException("Images must have the same dimensions: $f")
        }

        var diff = 0L
        for (y in 0 until image1.height) {
            for (x in 0 until image1.width) {
                diff += pixelDiff(image1.getPixel(x, y), image2.getPixel(x, y))
            }
        }
        val maxDiff = 3L * 255 * image1.width * image1.height
        Log.d(TAG, (100.0 * diff / maxDiff).toString())
        return 100.0 * diff / maxDiff
    }

    private fun pixelDiff(rgb1: Int, rgb2: Int): Int {
        val r1 = (rgb1 shr 16) and 0xff
        val g1 = (rgb1 shr 8) and 0xff
        val b1 = rgb1 and 0xff
        val r2 = (rgb2 shr 16) and 0xff
        val g2 = (rgb2 shr 8) and 0xff
        val b2 = rgb2 and 0xff
        return abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
    }
}