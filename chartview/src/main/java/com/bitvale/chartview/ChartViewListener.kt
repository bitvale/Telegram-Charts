package com.bitvale.chartview

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 10-Mar-19
 */
interface ChartViewListener {
    fun onDataSelected(data: Chart.ChartSelectedData)
    fun onDataCleared()
}