package ru.mirea.callcenter.util;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import ru.mirea.callcenter.model.Appeal;
import java.io.*; import java.nio.file.*; import java.util.List;
public class ExcelExporter {
    public void export(List<Appeal> appeals, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try(Workbook wb=new XSSFWorkbook();OutputStream out=Files.newOutputStream(file)) { Sheet sheet=wb.createSheet("Обращения");String[] h={"ID","Оператор","Клиент","Телефон","Тема","Категория","Статус","Создано","Закрыто"}; Row header=sheet.createRow(0);for(int i=0;i<h.length;i++)header.createCell(i).setCellValue(h[i]);int row=1;for(Appeal a:appeals){Row r=sheet.createRow(row++);String[] v={String.valueOf(a.getId()),String.valueOf(a.getOperatorId()),a.getClientName(),a.getClientPhone(),a.getTopic(),a.getCategory(),a.getStatus().name(),String.valueOf(a.getCreatedAt()),String.valueOf(a.getClosedAt())};for(int i=0;i<v.length;i++)r.createCell(i).setCellValue(v[i]);}for(int i=0;i<h.length;i++)sheet.autoSizeColumn(i);wb.write(out); }
    }
}
