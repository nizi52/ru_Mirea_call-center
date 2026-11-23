package ru.mirea.callcenter.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.callcenter.dto.AnalyticsDtos.AnalyticsResponse;
import ru.mirea.callcenter.dto.AnalyticsDtos.NamedCount;
import ru.mirea.callcenter.repository.AppealRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Формирует компактную аналитическую сводку на стороне базы. */
@Service
public class AnalyticsService {
    private final AppealRepository appeals;

    public AnalyticsService(AppealRepository appeals) {
        this.appeals = appeals;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        long total = appeals.count();
        long active = appeals.countByArchivedFalse();
        long archived = appeals.countByArchivedTrue();
        long withUrgency = appeals.countByArchivedFalseAndUrgentTrue();
        BigDecimal average = appeals.impactScoreForActive();
        if (average != null) average = average.setScale(2, RoundingMode.HALF_UP);
        return new AnalyticsResponse(total, active, archived, withUrgency, average,
                map(appeals.countActiveByCallCenter()), map(appeals.countActiveByStatus()), map(appeals.countActiveByPriority()));
    }

    private static List<NamedCount> map(List<Object[]> rows) {
        return rows.stream().map(row -> new NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue())).toList();
    }
}
