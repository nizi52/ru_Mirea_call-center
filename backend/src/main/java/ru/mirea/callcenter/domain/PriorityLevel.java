package ru.mirea.callcenter.domain;

/** Рассчитанный уровень приоритета обращения. */
public enum PriorityLevel {
    /** Приоритет не рассчитывается для завершённого/прекращённого обучения. */
    NOT_APPLICABLE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
