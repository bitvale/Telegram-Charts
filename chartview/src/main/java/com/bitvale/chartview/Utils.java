package com.bitvale.chartview;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 17-Mar-19
 */
public class Utils {
    public static float lerp(Float a, Float b, Float t) {
        return a + (b - a) * t;
    }
}
