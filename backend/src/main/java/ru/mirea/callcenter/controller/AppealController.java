package ru.mirea.callcenter.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.callcenter.dto.ImportDtos.ImportMode;
import ru.mirea.callcenter.dto.ImportDtos.ImportPreview;
import ru.mirea.callcenter.dto.ImportDtos.ImportResult;
import ru.mirea.callcenter.dto.PageResponse;
import ru.mirea.callcenter.dto.AppealDtos.BulkRequest;
import ru.mirea.callcenter.dto.AppealDtos.BulkResult;
import ru.mirea.callcenter.dto.AppealDtos.AppealRequest;
import ru.mirea.callcenter.dto.AppealDtos.AppealResponse;
import ru.mirea.callcenter.service.CsvAppealService;
import ru.mirea.callcenter.service.AppealService;
import ru.mirea.callcenter.web.PageableFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/** REST-контур управления обращениями. */
@RestController
@RequestMapping("/api/appeals")
@SecurityRequirement(name = "bearerAuth")
public class AppealController {
    private final AppealService appeals;
    private final CsvAppealService csv;
    private final PageableFactory pageableFactory;

    public AppealController(AppealService appeals, CsvAppealService csv, PageableFactory pageableFactory) {
        this.appeals = appeals;
        this.csv = csv;
        this.pageableFactory = pageableFactory;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public PageResponse<AppealResponse> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        return PageResponse.from(appeals.search(q, pageableFactory.appeals(page, size, sort)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public AppealResponse get(@PathVariable UUID id) {
        return appeals.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<AppealResponse> create(@Valid @RequestBody AppealRequest request) {
        return ResponseEntity.status(201).body(appeals.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public AppealResponse update(@PathVariable UUID id, @Valid @RequestBody AppealRequest request) {
        return appeals.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public AppealResponse archive(@PathVariable UUID id) {
        return appeals.archive(id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public AppealResponse restore(@PathVariable UUID id) {
        return appeals.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(
            @PathVariable UUID id,
            @RequestHeader(name = "X-Confirm-Delete", required = false) String confirmation) {
        appeals.permanentDelete(id, confirmation);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public BulkResult bulk(@Valid @RequestBody BulkRequest request) {
        return appeals.bulk(request);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ImportPreview previewImport(@RequestPart("file") MultipartFile file,
                                       @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ImportResult applyImport(@RequestPart("file") MultipartFile file,
                                    @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.apply(file, mode);
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public void export(@RequestParam(defaultValue = "") String q, HttpServletResponse response) throws IOException {
        String filename = "appeals-" + LocalDate.now() + ".csv";
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        csv.export(q, response.getOutputStream());
    }
}
