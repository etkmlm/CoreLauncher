package com.laeben.corelauncher.ui.util;

import java.text.DecimalFormat;

public class DisplayUtil {
    private static final String MB = "mb";
    private static final String KB = "kb";
    private static final int BYTES_PER_MB = 1024 * 1024;
    private static final int BYTES_PER_KB = 1024;

    private static final DecimalFormat formatSingleOptDigit = new DecimalFormat("0.#");

    /**
     * Formats the current value to string.
     * @param value current value in bytes
     * @return formatted value in kb or mb. ex: '23.2mb' or '320.3kb' or '300mb'
     */
    public static String parseDownloadProgress(long value){
        return value < BYTES_PER_MB ?
                formatSingleOptDigit.format(value * 1.0 / BYTES_PER_KB) + KB :
                formatSingleOptDigit.format(value * 1.0 / BYTES_PER_MB) + MB;
    }
}
