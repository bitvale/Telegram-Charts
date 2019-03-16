package com.bitvale.chartview;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 16-Mar-19
 */
public interface ChartViewListener {
    void onDataSelected(Chart.ChartSelectedData data);
    void onDataCleared();
}
