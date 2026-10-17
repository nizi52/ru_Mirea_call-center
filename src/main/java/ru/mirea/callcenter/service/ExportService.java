package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.model.Appeal;
import ru.mirea.callcenter.util.CsvExporter;
import ru.mirea.callcenter.util.ExcelExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ExportService {
    private final ExcelExporter excel = new ExcelExporter();
    private final CsvExporter csv = new CsvExporter();

    public void export(List<Appeal> appeals, Path file) throws IOException {
        if (appeals.isEmpty()) throw new BusinessException("Нет данных для экспорта: таблица пуста.");
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".xlsx")) excel.export(appeals, file);
        else if (name.endsWith(".csv")) csv.export(appeals, file);
        else throw new BusinessException("Поддерживаются только форматы .xlsx и .csv.");
    }
}
