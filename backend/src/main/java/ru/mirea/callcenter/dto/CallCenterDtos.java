package ru.mirea.callcenter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** DTO-контракты колл-центров. */
public final class CallCenterDtos {
    private CallCenterDtos() {
    }

    public record CallCenterRequest(
            @NotBlank @Size(max = 50) String code,
            @Size(max = 200) String name,
            @Min(1) @Max(720) Integer slaHours,
            Boolean active,
            Long version) {
    }

    public record CallCenterResponse(
            UUID id,
            String code,
            String name,
            Integer slaHours,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }
}
