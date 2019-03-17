package com.bitvale.telegramcharts;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bitvale.chartview.ChartViewListener;
import com.bitvale.chartview.model.Chart;
import com.bitvale.chartview.widget.chart.ChartContainer;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 17-Mar-19
 */
public class ChartFragment extends Fragment implements ChartViewListener {

    private SimpleDateFormat dayFormat = new SimpleDateFormat("E, MMM d", Locale.US);

    private static final String TITLE_EXTRA = "title_extra";
    private static final String CHART_EXTRA = "chart_extra";

    private View dataRoot;
    private LinearLayout dateContainer;
    private TextView date;

    static ChartFragment newInstance(String title, Chart chart) {
        ChartFragment fragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putString(TITLE_EXTRA, title);
        args.putParcelable(CHART_EXTRA, chart);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataRoot = view.findViewById(R.id.data_root);
        dateContainer = view.findViewById(R.id.date_container);
        date = view.findViewById(R.id.date);
        TextView title = view.findViewById(R.id.title);
        ChartContainer chartView = view.findViewById(R.id.chart);

        Bundle bundle = getArguments();
        if (bundle != null) {
            title.setText(bundle.getString(TITLE_EXTRA));
            Chart chartData = bundle.getParcelable(CHART_EXTRA);
            chartView.setupData(chartData);
            chartView.setChartViewListener(this);
        }
    }

    @Override
    public void onDataSelected(Chart.ChartSelectedData data) {
        if (dataRoot.getVisibility() != View.VISIBLE) dataRoot.setVisibility(View.VISIBLE);
        date.setText(dayFormat.format(data.date));
        LayoutInflater inflater = LayoutInflater.from(getContext());
        dateContainer.removeAllViews();
        for (int i = 0; i < data.values.size(); i++) {
            Chart.Data d = data.values.get(i);
            TextView tv = (TextView) inflater.inflate(R.layout.data_text_view, dateContainer, false);
            tv.setTextColor(Color.parseColor(d.color));
            tv.setText(String.valueOf(d.value));
            dateContainer.addView(tv);
        }
    }

    @Override
    public void onDataCleared() {
        dataRoot.setVisibility(View.GONE);
    }
}
