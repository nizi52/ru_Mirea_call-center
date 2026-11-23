package ru.mirea.callcenter.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.callcenter.config.AppProperties;
import ru.mirea.callcenter.domain.Appeal;
import ru.mirea.callcenter.domain.AppealStatus;
import ru.mirea.callcenter.domain.CallCenter;
import ru.mirea.callcenter.domain.UserAccount;
import ru.mirea.callcenter.dto.ImportDtos.ImportMode;
import ru.mirea.callcenter.dto.ImportDtos.ImportPreview;
import ru.mirea.callcenter.dto.ImportDtos.ImportResult;
import ru.mirea.callcenter.dto.ImportDtos.RowIssue;
import ru.mirea.callcenter.dto.AppealDtos.AppealRequest;
import ru.mirea.callcenter.dto.AppealDtos.AppealResponse;
import ru.mirea.callcenter.error.BadRequestException;
import ru.mirea.callcenter.filter.SmartFilterParser;
import ru.mirea.callcenter.filter.AppealSpecifications;
import ru.mirea.callcenter.repository.AppealRepository;
import ru.mirea.callcenter.repository.CallCenterRepository;
import ru.mirea.callcenter.security.CurrentUserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Защищённый CSV import/export.
 *
 * <p>Предварительная проверка колл-центрена от импорта: ошибки видны до записи в базу.
 * При экспорте нейтрализуются значения, которые Excel может воспринять как формулы.</p>
 */
@Service
public class CsvAppealService {
    private static final String[] HEADERS = {
            "externalId", "clientName", "topic", "category", "email", "phone",
            "dueDate", "center", "status", "impactScore", "urgent", "tags", "operatorId"
    };

    private final AppealRepository appeals;
    private final CallCenterRepository centers;
    private final AppealService appealService;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final AppealMapper mapper;
    private final Validator validator;
    private final AppProperties properties;
    private final AuditService audit;

    public CsvAppealService(AppealRepository appeals, CallCenterRepository centers,
                             AppealService appealService, CurrentUserService currentUser,
                             SmartFilterParser parser, AppealMapper mapper, Validator validator,
                             AppProperties properties, AuditService audit) {
        this.appeals = appeals;
        this.centers = centers;
        this.appealService = appealService;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.validator = validator;
        this.properties = properties;
        this.audit = audit;
    }

    /** Проверяет CSV без записи в базу. */
    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file, ImportMode mode) {
        ParsedFile parsed = parse(file, mode == null ? ImportMode.CREATE_ONLY : mode);
        return new ImportPreview(parsed.totalRows(), parsed.rows().size(), (int) parsed.issues().stream().map(RowIssue::row).distinct().count(), parsed.issues());
    }

    /** Применяет только валидные строки и возвращает детальный итог. */
    public ImportResult apply(MultipartFile file, ImportMode mode) {
        ImportMode effectiveMode = mode == null ? ImportMode.CREATE_ONLY : mode;
        ParsedFile parsed = parse(file, effectiveMode);
        int created = 0;
        int updated = 0;
        int skipped = (int) parsed.issues().stream().map(RowIssue::row).distinct().count();
        List<RowIssue> runtimeIssues = new ArrayList<>(parsed.issues());

        for (ParsedRow row : parsed.rows()) {
            try {
                Appeal existing = appeals.findByExternalIdIgnoreCase(row.request().externalId()).orElse(null);
                if (existing != null && effectiveMode == ImportMode.CREATE_ONLY) {
                    runtimeIssues.add(new RowIssue(row.rowNumber(), "externalId", "Запись уже существует"));
                    skipped++;
                    continue;
                }
                if (appealService.importRow(row.request(),effectiveMode==ImportMode.CREATE_ONLY)) created++; else updated++;
            } catch (RuntimeException ex) {
                runtimeIssues.add(new RowIssue(row.rowNumber(), "row", safeMessage(ex)));
                skipped++;
            }
        }
        audit.record("APPEAL_CSV_IMPORTED", "APPEAL", null, "Импортирован CSV со обращениями",
                Map.of("created", created, "updated", updated, "skipped", skipped, "mode", effectiveMode.name()));
        return new ImportResult(parsed.totalRows(), created, updated, skipped, List.copyOf(runtimeIssues));
    }

    /** Потоково экспортирует текущую выборку без загрузки всего набора в память. */
    @Transactional
    public void export(String query, OutputStream output) throws IOException {
        // BOM записывается до создания CSVPrinter, иначе заголовок окажется перед BOM.
        output.write("\ufeff".getBytes(StandardCharsets.UTF_8));
        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())) {
            int page = 0;
            while (true) {
                var result = appeals.findAll(AppealSpecifications.from(parser.parse(query)),
                        PageRequest.of(page, 500, Sort.by("clientName").ascending().and(Sort.by("topic").ascending())));
                for (Appeal appeal : result.getContent()) {
                    AppealResponse dto = mapper.toResponse(appeal);
                    printer.printRecord(
                            safeCell(dto.externalId()), safeCell(dto.clientName()), safeCell(dto.topic()),
                            safeCell(dto.category()), safeCell(dto.email()), safeCell(dto.phone()),
                            dto.dueDate(), safeCell(dto.centerCode()), dto.status(), dto.impactScore(),
                            dto.urgent(), safeCell(String.join("|", dto.tags())), dto.operatorId());
                }
                if (result.isLast()) break;
                page++;
            }
            printer.flush();
        }
        audit.record("APPEAL_CSV_EXPORTED", "APPEAL", null, "Экспортирована выборка обращений", Map.of("query", query == null ? "" : query));
    }

    private ParsedFile parse(MultipartFile file, ImportMode mode) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Выберите непустой CSV-файл");
        List<ParsedRow> validRows = new ArrayList<>();
        List<RowIssue> issues = new ArrayList<>();
        Set<String> externalIdsInFile = new LinkedHashSet<>();
        int total = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        try (Reader reader = bomReader(file);
             CSVParser csv = CSVParser.parse(reader, format)) {
            validateHeaders(csv.getHeaderMap());
            for (CSVRecord record : csv) {
                total++;
                long rowNumber = record.getRecordNumber() + 1;
                if (total > properties.importLimits().maxRows()) {
                    throw new BadRequestException("CSV содержит больше " + properties.importLimits().maxRows() + " строк");
                }
                try {
                    ParsedRow row = toRow(record, rowNumber);
                    Set<ConstraintViolation<AppealRequest>> violations = validator.validate(row.request());
                    if (!violations.isEmpty()) {
                        for (ConstraintViolation<AppealRequest> violation : violations) {
                            issues.add(new RowIssue(rowNumber, violation.getPropertyPath().toString(), violation.getMessage()));
                        }
                        continue;
                    }
                    String key = row.request().externalId().toLowerCase(Locale.ROOT);
                    if (!externalIdsInFile.add(key)) {
                        issues.add(new RowIssue(rowNumber, "externalId", "Дубликат externalId внутри файла"));
                        continue;
                    }
                    if (mode == ImportMode.CREATE_ONLY && appeals.existsByExternalIdIgnoreCase(row.request().externalId())) {
                        issues.add(new RowIssue(rowNumber, "externalId", "Запись уже существует"));
                        continue;
                    }
                    appealService.validateImport(row.request());
                    validRows.add(row);
                } catch (RuntimeException ex) {
                    issues.add(new RowIssue(rowNumber, "row", safeMessage(ex)));
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Не удалось прочитать CSV в UTF-8");
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Некорректная структура CSV: " + ex.getMessage());
        }
        return new ParsedFile(total, List.copyOf(validRows), List.copyOf(issues));
    }

    private Reader bomReader(MultipartFile file) throws IOException {
        java.io.PushbackReader reader=new java.io.PushbackReader(new InputStreamReader(file.getInputStream(),StandardCharsets.UTF_8),1);
        int ch=reader.read(); if(ch!=-1 && ch!=0xfeff) reader.unread(ch); return reader;
    }
    private ParsedRow toRow(CSVRecord record, long rowNumber) {
        String externalId = required(record, "externalId");
        String centerCode = nullable(record, "center");
        CallCenter center = centerCode == null ? null : centers.findByCodeIgnoreCase(centerCode)
                .orElseThrow(() -> new BadRequestException("Колл-центр не найдена: " + centerCode));
        if (center != null && !center.isActive()) throw new BadRequestException("Колл-центр неактивна: " + centerCode);

        AppealRequest request = new AppealRequest(
                externalId,
                required(record, "topic"),
                required(record, "clientName"),
                nullable(record, "category"),
                nullable(record, "email"),
                nullable(record, "phone"),
                parseDate(nullable(record, "dueDate")),
                center == null ? null : center.getId(),
                parseStatus(required(record, "status")),
                parseImpact(nullable(record, "impactScore")),
                parseBoolean(nullable(record, "urgent")),
                parseTags(nullable(record, "tags")),
                null, nullable(record,"operatorId")==null?null:Integer.valueOf(nullable(record,"operatorId")));
        return new ParsedRow(rowNumber, request);
    }

    private static void validateHeaders(Map<String, Integer> actual) {
        for (String header : HEADERS) {
            if (!actual.containsKey(header)) throw new BadRequestException("Отсутствует колонка: " + header);
        }
    }

    private static String required(CSVRecord record, String field) {
        String value = nullable(record, field);
        if (value == null) throw new BadRequestException("Не заполнено поле " + field);
        return value;
    }

    private static String nullable(CSVRecord record, String field) {
        String value = record.get(field);
        if(value!=null && value.startsWith("\'") && value.length()>1 && "=+-@\t\r".indexOf(value.charAt(1))>=0) value=value.substring(1);
        if (value == null) return null;
        value = value.replace("\ufeff", "").trim();
        return value.isEmpty() ? null : value;
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ex) { throw new BadRequestException("Дата должна иметь формат YYYY-MM-DD"); }
    }

    private static BigDecimal parseImpact(String value) {
        if (value == null) return null;
        try { return new BigDecimal(value.replace(',', '.')); }
        catch (NumberFormatException ex) { throw new BadRequestException("Некорректный impactScore"); }
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "да" -> true;
            case "false", "0", "no", "нет" -> false;
            default -> throw new BadRequestException("urgent принимает true/false или да/нет");
        };
    }

    private static AppealStatus parseStatus(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "new", "новое" -> AppealStatus.NEW;
            case "in_progress", "в_работе" -> AppealStatus.IN_PROGRESS;
            case "escalated", "эскалировано" -> AppealStatus.ESCALATED;
            case "resolved", "решено" -> AppealStatus.RESOLVED;
            case "closed", "закрыто" -> AppealStatus.CLOSED;
            default -> throw new BadRequestException("Неизвестный status: " + value);
        };
    }

    private static Set<String> parseTags(String value) {
        if (value == null) return Set.of();
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        Arrays.stream(value.split("\\|")).map(String::trim).filter(v -> !v.isEmpty())
                .map(v -> v.toLowerCase(Locale.ROOT)).forEach(tags::add);
        return tags;
    }

    private static String safeCell(Object value) {
        if (value == null) return "";
        String cell = String.valueOf(value);
        if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) return "'" + cell;
        return cell;
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return ex instanceof BadRequestException || ex instanceof ru.mirea.callcenter.error.ConflictException ? message : "Некорректная строка или конфликт данных";
    }

    private record ParsedRow(long rowNumber, AppealRequest request) { }
    private record ParsedFile(int totalRows, List<ParsedRow> rows, List<RowIssue> issues) { }
}
