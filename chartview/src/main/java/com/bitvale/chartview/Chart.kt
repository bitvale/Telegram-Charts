package com.bitvale.chartview

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 10-Mar-19
 */
@Parcelize
data class Chart(val columns: ArrayList<Column>): Parcelable {

    @Parcelize
    data class Column(val name: String, val type: Type, val color: String, val values: ArrayList<Long>,
                      var enabled: Boolean = true): Parcelable

    enum class Type(val value: String) {
        LINE("line"),
        X("x")
    }

    data class ChartSelectedData(val date: Long, val values: ArrayList<Data>)

    data class Data(val value: Long, val color: String)
}