package com.laeben.corelauncher.minecraft.util;

import java.util.Iterator;

public class VersionUtil {
    public static int calculateVersionValue(String version){
        if (version == null || version.isBlank()) return 0;

        final int len = version.length();

        int value = 0;
        byte shift = 3;
        byte dotCount = 0;
        byte dashCount = 0;
        int num = 0;

        boolean snapshotValueGiven = false;
        boolean writtenNum = false;

        for (int i = 0; i < len; i++){
            final char c = version.charAt(i);
            boolean ended = i == len - 1;
            boolean isDot = c == '.';
            boolean isDash = c == '-';
            boolean isNumber = c >= '0' && c <= '9';

            if (isNumber){
                byte v = (byte) (c - '0');
                num = num * 10 + v;

                writtenNum = true;
            }

            if ((!isNumber || ended) && writtenNum){
                value |= Math.min(num, 255) << (shift-- * 8);
                num = 0;

                writtenNum = false;
            }

            if (isDot) dotCount++;
            if (isDash) dashCount++;

            if (c == 'w' && dotCount == 0){
                return 0; // uncomparable snapshot ex: 25w02a
            }

            if (ended && dashCount == 0 && !snapshotValueGiven){
                value += 200; // base version distinction between snapshots
            }

            if (dashCount == 1 && !snapshotValueGiven){
                if (c == 'r') value += 100; // -rc
                else if (c == 'p') value += 50; // -pre
                // else value += 0 | -snapshot
                snapshotValueGiven = true;
            }

            if (isDash && dashCount == 2){
                shift = 0; // next numbers will be directly written into the first digit
                continue;
            }
        }

        return value;
    }
    public static String getNewestVersion(Iterator<String> versions){
        int value = -1;
        String version = null;
        while (versions.hasNext()){
            var v = versions.next();
            var n = calculateVersionValue(v);
            if (n > value){
                value = n;
                version = v;
            }
        }
        return version;
    }
}
