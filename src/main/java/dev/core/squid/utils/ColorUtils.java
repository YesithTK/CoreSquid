package dev.core.squid.utils;

public class ColorUtils {
    public static String color(String text) {
        if (text == null) return "";
        return text.replace("&", "\u00a7");
    }
}
