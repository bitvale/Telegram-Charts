package com.bitvale.telegramcharts

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.bitvale.chartview.Chart
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray


/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 09-Mar-19
 */
class MainActivity : AppCompatActivity() {

    // hard creation (without DI) for simplicity
    var prefs: Prefs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        setTheme(prefs?.theme!!)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val chartList = parseCharts()
        val adapter = ChartPagerAdapter(supportFragmentManager, chartList)
        pager.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.day_night -> switchMode()
        }
        return true
    }

    private fun switchMode() {
        prefs?.theme = if (prefs?.theme == R.style.AppThemeNight) R.style.AppThemeLight
        else R.style.AppThemeNight
        recreate()
    }

    private fun parseCharts(): ArrayList<Chart> {
        val jsonArray = JSONArray(loadJSONFromAsset())
        val chartList = ArrayList<Chart>()
        for (index in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(index)

            val columns = jsonObject.getJSONArray("columns")
            val types = jsonObject.getJSONObject("types")
            val names = jsonObject.getJSONObject("names")
            val colors = jsonObject.getJSONObject("colors")

            val chartColumns = ArrayList<Chart.Column>()

            for (columnIndex in 0 until columns.length()) {
                val valuesArray = columns[columnIndex] as JSONArray
                val columnName = valuesArray.get(0).toString()
                valuesArray.remove(0)
                val values: ArrayList<Long> = ArrayList()
                for (i in 0 until valuesArray.length()) {
                    values.add(valuesArray.getLong(i))
                }
                val type = Chart.Type.valueOf((types.get(columnName) as String).toUpperCase())
                val name = if (type == Chart.Type.LINE) names.get(columnName) as String
                else "x"
                val color = if (type == Chart.Type.LINE) colors.get(columnName) as String
                else ""
                val column = Chart.Column(name, type, color, values)
                chartColumns.add(column)
            }
            chartList.add(Chart(chartColumns))
        }
        return chartList
    }

    private fun loadJSONFromAsset(): String {
        val input = assets.open("chart_data.json")
        val size = input.available()
        val buffer = ByteArray(size)
        input.read(buffer)
        input.close()
        return String(buffer, Charsets.UTF_8)
    }
}
