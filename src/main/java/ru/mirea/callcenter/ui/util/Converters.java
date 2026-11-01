package ru.mirea.callcenter.ui.util;

import javafx.util.StringConverter;

import java.util.function.Function;

/** Как показывать объект в ComboBox: только текст для пользователя, без технических id. */
public final class Converters {
    private Converters() { }

    public static <T> StringConverter<T> of(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : toText.apply(value); }
            @Override public T fromString(String text) { return null; }
        };
    }
}
