package com.bitvale.telegramcharts;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.bitvale.chartview.model.Chart;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 17-Mar-19
 */
public class MainActivity extends AppCompatActivity {

    // hard creation (without DI) for simplicity
    Prefs prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = new Prefs(this);
        setTheme(prefs.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayList<Chart> chartList = parseCharts();
        ChartPagerAdapter adapter = new ChartPagerAdapter(getSupportFragmentManager(), chartList);
        ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.day_night:
                switchMode();
                break;
        }
        return true;
    }

    private void switchMode() {
        int theme = R.style.AppThemeNight;
        if (prefs.getTheme() == R.style.AppThemeNight) theme = R.style.AppThemeLight;
        prefs.setTheme(theme);
        recreate();
    }

    private ArrayList<Chart> parseCharts() {
        ArrayList<Chart> chartList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(loadJSONFromAsset());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                JSONArray columns = jsonObject.getJSONArray("columns");
                JSONObject types = jsonObject.getJSONObject("types");
                JSONObject names = jsonObject.getJSONObject("names");
                JSONObject colors = jsonObject.getJSONObject("colors");

                ArrayList<Chart.Column> chartColumns = new ArrayList<>();

                for (int j = 0; j < columns.length(); j++) {
                    JSONArray valuesArray = (JSONArray) columns.get(j);
                    String columnName = valuesArray.get(0).toString();
                    valuesArray.remove(0);
                    ArrayList<Long> values = new ArrayList<>();

                    for (int k = 0; k < valuesArray.length(); k++) {
                        values.add(valuesArray.getLong(k));
                    }

                    Chart.Type type = Chart.Type.valueOf(((String) types.get(columnName)).toUpperCase());
                    String name = "x";
                    if (type == Chart.Type.LINE) name = (String) names.get(columnName);

                    String color = "";
                    if (type == Chart.Type.LINE) color = (String) colors.get(columnName);

                    Chart.Column column = new Chart.Column(name, type, color, values);
                    chartColumns.add(column);
                }
                chartList.add(new Chart(chartColumns));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return chartList;
    }

    private String loadJSONFromAsset() {
        String json;
        try {
            InputStream input = getAssets().open("chart_data.json");
            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
