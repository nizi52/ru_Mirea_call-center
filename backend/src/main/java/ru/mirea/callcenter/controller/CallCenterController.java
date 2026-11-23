package ru.mirea.callcenter.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.callcenter.dto.CallCenterDtos.CallCenterRequest;
import ru.mirea.callcenter.dto.CallCenterDtos.CallCenterResponse;
import ru.mirea.callcenter.service.CallCenterService;

import java.util.List;
import java.util.UUID;

/** REST-контур колл-центров. */
@RestController
@RequestMapping("/api/centers")
@SecurityRequirement(name = "bearerAuth")
public class CallCenterController {
    private final CallCenterService centers;

    public CallCenterController(CallCenterService centers) { this.centers = centers; }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public List<CallCenterResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return centers.list(activeOnly);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CallCenterResponse> create(@Valid @RequestBody CallCenterRequest request) {
        return ResponseEntity.status(201).body(centers.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CallCenterResponse update(@PathVariable UUID id, @Valid @RequestBody CallCenterRequest request) {
        return centers.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public CallCenterResponse setActive(@PathVariable UUID id, @RequestParam boolean active, @RequestParam long version) {
        return centers.setActive(id, active, version);
    }
}
