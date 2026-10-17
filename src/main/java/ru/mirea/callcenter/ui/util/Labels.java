package ru.mirea.callcenter.ui.util;

import ru.mirea.callcenter.model.AppealStatus;
import ru.mirea.callcenter.service.AppealFilter.SortField;

public final class Labels {
    private Labels() { }

    public static String status(AppealStatus s) {
        if (s == null) return "";
        return switch (s) {
            case NEW -> "Новое";
            case IN_PROGRESS -> "В работе";
            case ESCALATED -> "Эскалировано";
            case RESOLVED -> "Решено";
            case CLOSED -> "Закрыто";
        };
    }

    public static String category(String code) {
        if (code == null) return "";
        return switch (code) {
            case "TECH" -> "Техническая";
            case "COMPLAINT" -> "Жалоба";
            case "CONSULTATION" -> "Консультация";
            case "BILLING" -> "Оплата";
            default -> code;
        };
    }

    public static String sort(SortField f) {
        if (f == null) return "";
        return switch (f) {
            case ID -> "По умолчанию";
            case CREATED_NEWEST -> "Сначала новые";
            case CREATED_OLDEST -> "Сначала старые";
            case STATUS -> "По статусу";
        };
    }
}
