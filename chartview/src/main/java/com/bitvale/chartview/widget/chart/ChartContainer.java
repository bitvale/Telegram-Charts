package com.bitvale.chartview.widget.chart;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.bitvale.chartview.ChartViewListener;
import com.bitvale.chartview.widget.ChipView;
import com.bitvale.chartview.R;
import com.bitvale.chartview.model.Chart;

import java.util.ArrayList;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 16-Mar-19
 */
public class ChartContainer extends LinearLayout {

    private ChartView chartView;
    private ChartSpinner chartSpinner;
    private LinearLayout controlsContainer;

    public ChartContainer(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ChartContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ChartContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setOrientation(LinearLayout.VERTICAL);

        chartView = new ChartView(context, attrs, defStyleAttr);

        chartSpinner = new ChartSpinner(context, attrs, defStyleAttr);
        chartSpinner.setChartListener(chartView);

        controlsContainer = new LinearLayout(context);
        controlsContainer.setOrientation(LinearLayout.HORIZONTAL);

        int margin = context.getResources().getDimensionPixelSize(R.dimen.def_margin);

        addView(chartView);
        addView(chartSpinner);
        ((MarginLayoutParams) chartSpinner.getLayoutParams()).setMargins(0, margin / 2, 0, 0);
        addView(controlsContainer);
        ((MarginLayoutParams) controlsContainer.getLayoutParams()).setMargins(0, margin, 0, 0);
    }

    public void setupData(Chart chart) {
        ArrayList<Long> xAxis = chart.columns.get(0).values;
        ArrayList<Chart.Column> yAxis = new ArrayList<>();
        for (int i = 1; i < chart.columns.size(); i++) {
            yAxis.add(chart.columns.get(i));
        }
        chartView.setupData(xAxis, yAxis);
        chartSpinner.setupData(xAxis, yAxis);
        createControls(chart);
    }

    private void createControls(Chart chart) {
        controlsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 1; i < chart.columns.size(); i++) {
            ChipView chip = (ChipView) inflater.inflate(R.layout.chip_view, this, false);
            Chart.Column column = chart.columns.get(i);
            chip.setChecked(column.enabled);
            chip.setText(column.name);
            chip.setCheckedColor(Color.parseColor(column.color));
            chip.setOnClickListener(v -> {
                int enabled = 0;
                if (chip.isChecked()) {
                    for (int j = 1; j < chart.columns.size(); j++) {
                        if (chart.columns.get(j).enabled) {
                            enabled++;
                        }
                    }
                }
                if (enabled == 1) return;

                column.enabled = !column.enabled;
                Chart.ChartAnimation animation = Chart.ChartAnimation.DOWN;
                if (chip.isChecked()) animation = Chart.ChartAnimation.UP;
                column.animation = animation;

                chartView.animateInOut(chip.isChecked());
                chartSpinner.animateInOut(chip.isChecked());
                chip.animateChecked();
                chartSpinner.invalidate();
            });
            controlsContainer.addView(chip);
        }
    }

    public void setChartViewListener(ChartViewListener listener) {
        chartView.setChartViewListener(listener);
    }
}
