package com.bitvale.chartview

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 10-Mar-19
 */
interface ChartSpinnerListener {
    fun onRangeChanged(daysBeforeFrame: Int, daysAfterFrame: Int, daysInFrame: Int, dx: Float)
}