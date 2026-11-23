package ru.mirea.callcenter.filter;

import ru.mirea.callcenter.domain.PriorityLevel;
import ru.mirea.callcenter.domain.AppealStatus;

import java.math.BigDecimal;
import java.util.List;

/** Результат безопасного разбора строки умного фильтра. */
public record AppealFilter(
        String freeText,
        String center,
        AppealStatus status,
        Boolean urgency,
        PriorityLevel priority,
        String tag,
        Boolean archived,
        List<ImpactConstraint> impacts) {

    public enum Operator { LT, LTE, EQ, GTE, GT }
    public record ImpactConstraint(Operator operator, BigDecimal value) { }
}
