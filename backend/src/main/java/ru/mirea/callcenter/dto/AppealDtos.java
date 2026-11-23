package ru.mirea.callcenter.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.mirea.callcenter.domain.PriorityLevel;
import ru.mirea.callcenter.domain.AppealStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTO-контракты обращений и массовых операций. */
public final class AppealDtos {
    private AppealDtos() {
    }

    /** Входная модель создания и изменения карточки. */
    public record AppealRequest(
            @NotBlank @Size(max = 100) String externalId,
            @NotBlank @Size(max = 300) String topic,
            @NotBlank @Size(max = 150) String clientName,
            @NotBlank @Pattern(regexp = "TECH|COMPLAINT|CONSULTATION|BILLING") String category,
            @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = "^\\+?[0-9() -]{7,30}$", message = "некорректный телефон") String phone,
            LocalDate dueDate,
            @NotNull UUID centerId,
            @NotNull AppealStatus status,
            @jakarta.validation.constraints.Digits(integer=3, fraction=2) @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal impactScore,
            boolean urgent,
            @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
            Long version, Integer operatorId) {
    }

    /** Выходная карточка обращения. */
    public record AppealResponse(
            UUID id,
            String externalId,
            String topic,
            String clientName,
            String category,
            String fullName,
            String email,
            String phone,
            LocalDate dueDate,
            UUID centerId,
            String centerCode,
            AppealStatus status,
            BigDecimal impactScore,
            boolean urgent,
            PriorityLevel priorityLevel,
            boolean archived,
            Instant archivedAt,
            Set<String> tags,
            Instant createdAt,
            Instant updatedAt,
            long version, Integer operatorId) {
    }

    /** Поддерживаемые массовые действия. */
    public enum BulkAction { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }

    /** Запрос массового изменения. */
    public record BulkRequest(
            @NotNull @Size(min = 1, max = 500) Set<UUID> ids,
            @NotNull BulkAction action,
            AppealStatus status,
            @Size(max = 50) String tag) {
    }

    /** Результат массовой операции с явным списком ошибок. */
    public record BulkResult(int requested, int succeeded, List<BulkFailure> failures) {
    }

    /** Ошибка отдельной записи в массовой операции. */
    public record BulkFailure(UUID id, String message) {
    }
}
