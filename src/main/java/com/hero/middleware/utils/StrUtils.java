package com.hero.middleware.utils;

public final class StrUtils {

    private StrUtils() {
    }

    public static String removeAllSpaces(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\s\\u3000]+", "");
    }

    public static String substringByLength(String value, int startIndex, int length) {
        if (value == null) {
            return null;
        }
        if (length <= 0) {
            return "";
        }
        int safeStartIndex = Math.max(0, Math.min(startIndex, value.length()));
        int endIndex = Math.min(safeStartIndex + length, value.length());
        return value.substring(safeStartIndex, endIndex);
    }
}
