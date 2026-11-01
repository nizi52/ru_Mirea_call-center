package ru.mirea.callcenter.service;

import ru.mirea.callcenter.model.Appeal;
import ru.mirea.callcenter.model.AppealStatus;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public record AppealStatistics(int total, long open, Map<AppealStatus, Long> byStatus,
                               Map<String, Long> byCategory, Map<String, Long> byOperator,
                               double avgCloseMinutes, double escalatedPercent) {

    public static AppealStatistics of(List<Appeal> appeals) {
        Map<AppealStatus, Long> byStatus = appeals.stream().collect(Collectors.groupingBy(
                Appeal::getStatus, () -> new EnumMap<>(AppealStatus.class), Collectors.counting()));
        Map<String, Long> byCategory = appeals.stream().collect(Collectors.groupingBy(
                Appeal::getCategory, TreeMap::new, Collectors.counting()));
        Map<String, Long> byOperator = appeals.stream().filter(a -> a.getOperatorName() != null)
                .collect(Collectors.groupingBy(Appeal::getOperatorName, TreeMap::new, Collectors.counting()));
        double avgClose = appeals.stream().filter(a -> a.getClosedAt() != null)
                .mapToLong(a -> Duration.between(a.getCreatedAt(), a.getClosedAt()).toMinutes())
                .average().orElse(0);
        long escalated = byStatus.getOrDefault(AppealStatus.ESCALATED, 0L);
        double escalatedPercent = appeals.isEmpty() ? 0 : escalated * 100.0 / appeals.size();
        long open = appeals.stream().filter(a -> a.getStatus() != AppealStatus.CLOSED).count();
        return new AppealStatistics(appeals.size(), open, byStatus, byCategory, byOperator, avgClose, escalatedPercent);
    }
}
