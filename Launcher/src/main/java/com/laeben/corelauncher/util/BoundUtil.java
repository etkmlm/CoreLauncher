package com.laeben.corelauncher.util;

public class BoundUtil {
    public static double getIntersectionLength(double objMin, double objMax, double min, double max){
        if (objMax < min) return 0;

        if (max <= objMax){
            if (objMin <= min) return max - min;
            else return max - objMin;
        }
        else {
            if (objMin <= min) return objMax - min;
            else return objMax - objMin;
        }
    }
}
