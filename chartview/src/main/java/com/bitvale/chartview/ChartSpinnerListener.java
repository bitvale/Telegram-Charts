package com.bitvale.chartview;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 16-Mar-19
 */
public interface ChartSpinnerListener {
    void onRangeChanged(int daysBeforeFrame, int daysAfterFrame, int daysInFrame);
}
