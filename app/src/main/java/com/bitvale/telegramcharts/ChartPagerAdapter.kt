package com.bitvale.telegramcharts

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.bitvale.chartview.model.Chart

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 11-Mar-19
 */
class ChartPagerAdapter(fragmentManager: FragmentManager, private val chartList: ArrayList<Chart>)
    : FragmentPagerAdapter(fragmentManager) {

    override fun getCount(): Int = chartList.size

    override fun getItem(position: Int): Fragment {
        return ChartFragment.newInstance("Chart ${position + 1}", chartList[position])
    }
}