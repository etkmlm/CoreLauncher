package com.laeben.corelauncher.api.util;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
    public static Date fromString(String date) {
        return date == null ? Date.from(Instant.ofEpochSecond(0)) : Date.from(Instant.parse(date));
    }

    public static String toString(Date date, Locale locale) {
        return date == null ? "" : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(date);
    }
}
