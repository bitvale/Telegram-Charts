package com.bitvale.chartview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.updateMargins

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 10-Mar-19
 */
class ChartContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var chartView = ChartView(context, attrs, defStyleAttr)
    private var chartSpinner = ChartSpinner(context, attrs, defStyleAttr)
    private var controlsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }
    private val margin = context.resources.getDimensionPixelSize(R.dimen.def_margin)

    init {
        orientation = LinearLayout.VERTICAL
        chartSpinner.setChartListener(chartView)
        addView(chartView)
        addView(chartSpinner)
        (chartSpinner.layoutParams as MarginLayoutParams).updateMargins(top = margin)
        addView(controlsContainer)
        (controlsContainer.layoutParams as MarginLayoutParams).updateMargins(top = margin)
    }

    fun setupData(chart: Chart) {
        chartView.setupData(chart)
        chartSpinner.setupData(chart)
        createControls(chart)
    }

    private fun createControls(chart: Chart) {
        controlsContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for (i in 1 until chart.columns.size) {
            val chip: ChipView = inflater.inflate(R.layout.chip_view, this, false) as ChipView
            chip.isChecked = chart.columns[i].enabled
            chip.setText(chart.columns[i].name)
            chip.setCheckedColor(Color.parseColor(chart.columns[i].color))
            chip.setOnClickListener {
                chart.columns[i].enabled = !chart.columns[i].enabled
                chip.animateChecked()
                chartView.invalidate()
                chartSpinner.invalidate()
            }
            controlsContainer.addView(chip)
        }
    }

    fun setChartViewListener(listener: ChartViewListener) {
        chartView.setChartViewListener(listener)
    }
}