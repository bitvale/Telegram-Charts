package com.bitvale.chartview

import android.text.StaticLayout

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 11-Mar-19
 */
fun StaticLayout.textWidth(): Int {
    var width = 0f
    for (i in 0 until lineCount) {
        width = width.coerceAtLeast(getLineWidth(i))
    }
    return width.toInt()
}

fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}