package com.mexator.camya.util.functions

import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.SurfaceHolder

fun getSmallestResolution(characteristics: CameraCharacteristics): Size {
    val confMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    val sizes = confMap!!.getOutputSizes(SurfaceHolder::class.java)
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