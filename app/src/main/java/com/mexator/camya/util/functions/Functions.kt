package com.mexator.camya.util.functions

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Size

fun getSmallestResolution(characteristics: CameraCharacteristics): Size {
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