package dev.core.squid.utils;

public class ColorUtils {
    public static String color(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "\u00a7");
    }
}
