package com.bitvale.chartview.utils


/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 09-Mar-19
 */
class MaxSizeHashMap<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>() {

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > maxSize
    }
}