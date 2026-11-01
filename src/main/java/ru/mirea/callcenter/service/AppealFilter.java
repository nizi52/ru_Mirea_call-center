package ru.mirea.callcenter.service;

import ru.mirea.callcenter.model.AppealStatus;
import java.time.LocalDate;

/** Параметры выборки обращений. Пустое значение (null) означает «не фильтровать». */
public record AppealFilter(String text, AppealStatus status, String category,
                           LocalDate from, LocalDate to, SortField sort, boolean activeOnly) {

    public enum SortField { ID, CREATED_NEWEST, CREATED_OLDEST, STATUS }

    /** Без галочки «Только активные». */
    public AppealFilter(String text, AppealStatus status, String category, LocalDate from, LocalDate to, SortField sort) {
        this(text, status, category, from, to, sort, false);
    }

    public static AppealFilter none() {
        return new AppealFilter(null, null, null, null, null, SortField.ID, false);
    }
}
