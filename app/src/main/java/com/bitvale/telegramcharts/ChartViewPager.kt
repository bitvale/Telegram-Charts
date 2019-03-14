package com.bitvale.telegramcharts

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.bitvale.chartview.ChartSpinner

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 11-Mar-19
 */
class ChartViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    override fun canScroll(v: View?, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        if (v is ChartSpinner) return true
        return super.canScroll(v, checkV, dx, x, y)
    }
}
