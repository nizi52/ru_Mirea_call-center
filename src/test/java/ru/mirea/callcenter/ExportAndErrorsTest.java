package ru.mirea.callcenter;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.model.Appeal;
import ru.mirea.callcenter.model.AppealStatus;
import ru.mirea.callcenter.service.ExportService;
import ru.mirea.callcenter.util.DbErrors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportAndErrorsTest {
    private final ExportService export = new ExportService();

    private static Appeal sample() {
        Appeal a = new Appeal(1, 2, 3, "Иван Петров", "+79990000001", "Не работает интернет", "TECH",
                AppealStatus.IN_PROGRESS, LocalDateTime.of(2026, 9, 1, 9, 0), null);
        a.setOperatorName("Анна Иванова");
        return a;
    }

    @Test
    void exportsCsvWithOperatorNameAndBom(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("appeals.csv");

        export.export(List.of(sample()), file);

        String text = Files.readString(file);
        assertThat(text).startsWith("﻿");
        assertThat(text).contains("Анна Иванова").contains("Не работает интернет");
    }

    @Test
    void exportsXlsxWithHeaderAndRows(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("appeals.xlsx");

        export.export(List.of(sample(), sample()), file);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file))) {
            assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(3);
        }
    }

    @Test
    void rejectsEmptyListAndUnknownExtension(@TempDir Path dir) {
        assertThatThrownBy(() -> export.export(List.of(), dir.resolve("a.csv")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("таблица пуста");
        assertThatThrownBy(() -> export.export(List.of(sample()), dir.resolve("a.txt")))
                .isInstanceOf(BusinessException.class).hasMessageContaining(".xlsx");
    }

    @Test
    void databaseErrorsAreTranslatedToUserMessages() {
        assertThat(DbErrors.toMessage(new SQLException("x", "23505"))).contains("уникальным");
        assertThat(DbErrors.toMessage(new SQLException("x", "23503"))).contains("связана");
        assertThat(DbErrors.toMessage(new SQLException("x", "08001"))).contains("подключиться");
        assertThat(DbErrors.toMessage(new SQLException("x", "99999"))).doesNotContain("SQL");
    }
}
