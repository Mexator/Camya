package com.mexator.camya.extensions

import android.util.Size

fun Any.getTag() = this.javaClass.simpleName

fun Size.toPair() = Pair(this.height, this.width)