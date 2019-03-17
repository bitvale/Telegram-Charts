package com.bitvale.telegramcharts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bitvale.chartview.model.Chart
import com.bitvale.chartview.ChartViewListener
import kotlinx.android.synthetic.main.fragment_chart.*
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 11-Mar-19
 */
class ChartFragment : Fragment(), ChartViewListener {

    private val dayFormat = SimpleDateFormat("E, MMM d", Locale.US)

    companion object {
        private const val TITLE_EXTRA = "title_extra"
        private const val CHART_EXTRA = "chart_extra"

        fun newInstance(title: String, chart: Chart): ChartFragment {
            val fragment = ChartFragment()
            val args = Bundle()
            args.putString(TITLE_EXTRA, title)
            args.putParcelable(CHART_EXTRA, chart)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { bundle ->
            title.text = bundle.getString(TITLE_EXTRA)
            val chartData: Chart? = bundle.getParcelable(CHART_EXTRA)
            chartData?.let { chart.setupData(it) }
        }
        chart.setChartViewListener(this)
    }

    override fun onDataSelected(data: Chart.ChartSelectedData) {
       if (data_root.visibility != View.VISIBLE) data_root.visibility = View.VISIBLE
        date.text = dayFormat.format(data.date)
        val inflater = LayoutInflater.from(context)
        date_container.removeAllViews()
        data.values.forEach {
            val tv = inflater.inflate(R.layout.data_text_view, date_container, false) as TextView
            tv.setTextColor(Color.parseColor(it.color))
            tv.text = it.value.toString()
            date_container.addView(tv)
        }
    }

    override fun onDataCleared() {
        data_root.visibility = View.GONE
    }
}