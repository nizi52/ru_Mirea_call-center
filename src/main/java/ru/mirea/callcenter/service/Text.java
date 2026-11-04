package ru.mirea.callcenter.service;

import java.util.Locale;

/** Мелкие операции со строками для валидации и поиска в сервисах. */
final class Text {
    private Text() { }

    static String trim(String s) { return s == null ? "" : s.trim(); }

    static String normalize(String s) { return trim(s).toLowerCase(Locale.ROOT); }

    static boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
}
