package com.bitvale.telegramcharts;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import com.bitvale.chartview.widget.chart.ChartSpinner;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 18-Mar-19
 */
public class ChartViewPager extends ViewPager {
    public ChartViewPager(@NonNull Context context) {
        super(context);
    }

    public ChartViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ChartSpinner) return true;
        return super.canScroll(v, checkV, dx, x, y);
    }
}
