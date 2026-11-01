package ru.mirea.callcenter.util;

import java.sql.SQLException;

public final class DbErrors {
    private DbErrors() { }

    public static String toMessage(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        return switch (state) {
            case "23505" -> "Такая запись уже существует: значение должно быть уникальным.";
            case "23503" -> "Операция невозможна: запись связана с другими данными.";
            case "23514" -> "Данные нарушают ограничение базы данных. Проверьте заполненные поля.";
            default -> state.startsWith("08")
                    ? "Не удалось подключиться к базе данных. Проверьте, что PostgreSQL запущен, а настройки в database.properties верны."
                    : "Ошибка при работе с базой данных.";
        };
    }
}
