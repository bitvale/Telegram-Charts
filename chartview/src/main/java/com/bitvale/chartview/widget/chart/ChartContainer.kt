package com.bitvale.chartview.widget.chart

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.updateMargins
import com.bitvale.chartview.model.Chart
import com.bitvale.chartview.ChartViewListener
import com.bitvale.chartview.widget.ChipView
import com.bitvale.chartview.R

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
        (chartSpinner.layoutParams as MarginLayoutParams).updateMargins(top = margin / 2)
        addView(controlsContainer)
        (controlsContainer.layoutParams as MarginLayoutParams).updateMargins(top = margin)
    }

    fun setupData(chart: Chart) {
        val xAxis = chart.columns[0].values
        val yAxis = ArrayList<Chart.Column>()
        for (i in 1 until chart.columns.size) {
            yAxis.add(chart.columns[i])
        }
        chartView.setupData(xAxis, yAxis)
        chartSpinner.setupData(xAxis, yAxis)
        createControls(chart)
    }

    private fun createControls(chart: Chart) {
        controlsContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for (i in 1 until chart.columns.size) {
            val chip: ChipView = inflater.inflate(R.layout.chip_view, this, false) as ChipView
            val column = chart.columns[i]
            chip.isChecked = column.enabled
            chip.setText(column.name)
            chip.setCheckedColor(Color.parseColor(column.color))
            chip.setOnClickListener {
                var enabled = 0
                if (chip.isChecked) {
                    for (j in 1 until chart.columns.size) {
                        if (chart.columns[j].enabled) {
                            enabled++
                        }
                    }
                }
                if (enabled == 1) return@setOnClickListener

                column.enabled = !column.enabled
                column.animation = if (chip.isChecked) Chart.ChartAnimation.UP else Chart.ChartAnimation.DOWN
                chartView.animateInOut(chip.isChecked)
                chip.animateChecked()
                chartSpinner.invalidate()
            }
            controlsContainer.addView(chip)
        }
    }

    fun setChartViewListener(listener: ChartViewListener) {
        chartView.setChartViewListener(listener)
    }
}