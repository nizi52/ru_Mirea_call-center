package ru.mirea.callcenter.exception;

public class BusinessException extends RuntimeException {
    private final String field;

    public BusinessException(String message) { this(null, message); }

    /** field — код поля формы, с которым связана ошибка (client, topic, category, operator). */
    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
