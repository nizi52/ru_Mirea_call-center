package ru.mirea.callcenter.domain;

/** Состояние обучения обращения. */
public enum AppealStatus {
    NEW,
    IN_PROGRESS,
    ESCALATED,
    RESOLVED,
    CLOSED
}
