package ru.mirea.callcenter.util;

import ru.mirea.callcenter.model.Appeal;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvExporter {
    public void export(List<Appeal> appeals, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("id;operator_id;client_id;client_name;client_phone;topic;category;status;created_at;closed_at");
            writer.newLine();
            for (Appeal appeal : appeals) {
                String[] values = {text(appeal.getId()), text(appeal.getOperatorId()), text(appeal.getClientId()),
                        appeal.getClientName(), appeal.getClientPhone(), appeal.getTopic(), appeal.getCategory(),
                        appeal.getStatus().name(), text(appeal.getCreatedAt()), text(appeal.getClosedAt())};
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) writer.write(';');
                    writer.write(escape(values[i]));
                }
                writer.newLine();
            }
        }
    }

    private String text(Object value) { return value == null ? "" : value.toString(); }
    private String escape(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
