package com.bitvale.chartview

import com.bitvale.chartview.model.Chart

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 10-Mar-19
 */
interface ChartViewListener {
    fun onDataSelected(data: Chart.ChartSelectedData)
    fun onDataCleared()
}