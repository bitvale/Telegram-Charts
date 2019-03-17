package com.bitvale.telegramcharts;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.bitvale.chartview.model.Chart;

import java.util.ArrayList;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 17-Mar-19
 */
public class ChartPagerAdapter extends FragmentPagerAdapter {

    private ArrayList<Chart> chartList;

    ChartPagerAdapter(@NonNull FragmentManager fm, ArrayList<Chart> chartList) {
        super(fm);
        this.chartList = chartList;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return ChartFragment.newInstance("Chart " + (position + 1), chartList.get(position));
    }

    @Override
    public int getCount() {
        return chartList.size();
    }
}
