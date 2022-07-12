package com.uci.utils.bot.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    /**
     * Global date time format
     * @return
     */
    public static String getDateTimeFormat() {
        return "yyyy-MM-dd'T'HH:mm:ss'Z'";
    }

    /**
     * Convert local date time to global format
     * @param dateTime
     * @return
     */
    public static String convertLocalDateTimeToFormat(LocalDateTime dateTime) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(getDateTimeFormat());
        return fmt.format(dateTime).toString();
    }
}
