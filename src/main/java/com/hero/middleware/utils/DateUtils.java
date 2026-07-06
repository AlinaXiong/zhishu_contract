package com.hero.middleware.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    /**
     * lang类型转换日期格式
     * @param millis
     * @param format
     * @return
     */
    public static String convertMillisToDate(long millis, String format) {
        if(format==null){
            format="yyyy-MM-dd HH:mm:ss";
        }
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return dateTime.format(formatter);
    }

    public static String convertDateToString(Date date, String format) {
        if(format==null){
            format="yyyy-MM-dd HH:mm:ss";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static Date convertStringToDate(String date, String format) {
        if(date==null){
            return null;
        }
        if(format==null){
            format="yyyy-MM-dd HH:mm:ss";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
