package com.cdev.corelauncher.utils;

import java.io.File;

public class StringUtils {

    private static final char[] INVALID_CHARS = {
            34,60,62,124,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,58,42,63,92,47
    };

    public static String trimEnd(String str, char c){
        var n = new StringBuilder();

        boolean x = false;
        int len = str.length();
        int t = 0;

        for (int i = len - 1; i >= 0; i--){
            if (str.charAt(i) != c)
                x = true;

            if (x)
                n.append(str.charAt(len - i - 1 - t));
            else
                t++;
        }

        return n.toString();
    }

    public static String pure(String source){
        if (source == null)
            return null;
        for(char i : INVALID_CHARS)
            source = source.replace(String.valueOf(i), "");
        return source;
    }
}
