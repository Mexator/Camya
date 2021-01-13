package com.mexator.camya.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.graphics.scale
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
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
class MovementDetector(inputSize: Size) {
    companion object {
        private const val TAG = "MovementDetector"

        /** Threshold in percents to detect move **/
        private const val THRESHOLD = 10

        /** Image from camera is downscaled to SCALE_SIZE x SCALE_SIZE to compute pixel difference **/
        private const val SCALE_SIZE = 32
    }

    val surface: Surface
    val isDetected: Observable<Boolean>

    /** [HandlerThread] where all buffer reading operations run */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    /** [Handler] corresponding to [imageReaderThread] */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    private var prevImage: Bitmap? = null
        set(value) {
            field?.recycle()
            field = value
        }
    private val imageReader: ImageReader = ImageReader
        .newInstance(
            inputSize.width,
            inputSize.height,
            // I know that JPEG adds serious overhead, but the device I need to run this app at
            // is unable to record YUV by whatever reason. See https://stackoverflow.com/q/65693311/
            ImageFormat.JPEG,
            2
        )

    private val imagePostingSubject: BehaviorSubject<Bitmap> = BehaviorSubject.create()
    private val listener = ImageReader.OnImageAvailableListener { reader ->
        // The plan is following: acquire image, put it to the subject, and check subject
        // periodically with Observable.sample()
        reader.acquireLatestImage()?.let { image ->
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val data = ByteArray(buffer.capacity())
            buffer.get(data)
            image.close()

            var bitmap: Bitmap = BitmapFactory
                .decodeByteArray(data, 0, data.size)
            bitmap = bitmap.scale(SCALE_SIZE, SCALE_SIZE)

            imagePostingSubject.onNext(bitmap)
        }
    }

    private val compositeDisposable = CompositeDisposable()

    init {
        surface = imageReader.surface
        imageReader.setOnImageAvailableListener(listener, imageReaderHandler)
        val job = imagePostingSubject
            .sample(1, TimeUnit.SECONDS)
            .observeOn(Schedulers.computation())
            .subscribe {
                // To make sure that prevImage won't be null in if body
                val mPrevImage = prevImage
                if (mPrevImage == null) {
                    _isDetected.onNext(false)
                } else {
                    val difference = pixelDiffPercent(mPrevImage, it)
                    _isDetected.onNext(difference > THRESHOLD)
                }
                prevImage = it
            }
        compositeDisposable.add(job)
    }

    private val _isDetected: PublishSubject<Boolean> = PublishSubject.create()

    init {
        isDetected = _isDetected
    }

    fun release() {
        imageReader.close()
        prevImage?.recycle()
        compositeDisposable.dispose()
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