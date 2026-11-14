package com.rms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.rms.dto.AssignmentView;
import com.rms.entity.AssignmentStatus;
import com.rms.error.ApiErrors;
import com.rms.repository.AssignmentRepository;
import com.rms.mapper.Views;
import com.rms.util.TimeUtil;

/** Builds the admin "daily schedule" workbook (XLSX), reproducing the reference styling. */
@Service
public class DailyReportService {

    private static final String[] HEADERS = {
            "Time", "Worker", "Client", "Room", "Location", "Duration (min)", "Status", "Notes"
    };

    private final AssignmentRepository assignmentRepository;

    public DailyReportService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public byte[] generate(LocalDate day) {
        TimeUtil.DayBounds bounds = TimeUtil.dayBounds(day);
        List<AssignmentView> rows = assignmentRepository.findInRange(bounds.start(), bounds.end()).stream()
                .map(Views::assignment).toList();

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Daily Schedule");

            CellStyle title = titleStyle(wb);
            CellStyle subtitle = subtitleStyle(wb);
            CellStyle header = headerStyle(wb);
            CellStyle body = bodyStyle(wb);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("Daily Schedule — " + day);
            c0.setCellStyle(title);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Generated " + Views.formatLocal(java.time.LocalDateTime.now()) + " · " + rows.size() + " shifts");
            c1.setCellStyle(subtitle);

            Row hr = sheet.createRow(2);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = hr.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(header);
            }

            int rowIndex = 3;
            for (AssignmentView a : rows) {
                Row row = sheet.createRow(rowIndex++);
                write(row, 0, a.start() + " – " + a.end(), body);
                write(row, 1, a.workerName(), body);
                write(row, 2, a.clientName(), body);
                write(row, 3, a.roomNumber(), body);
                write(row, 4, a.location(), body);
                write(row, 5, Integer.toString(a.durationMinutes()), body);
                Cell statusCell = row.createCell(6);
                statusCell.setCellValue(a.status().name());
                statusCell.setCellStyle(statusStyle(wb, a.status()));
                write(row, 7, a.notes() == null ? "" : a.notes(), body);
            }

            sheet.createFreezePane(0, 3);
            autoSize(sheet);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw ApiErrors.reportGenerationFailed(ex.getMessage());
        }
    }

    private static void write(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(neutralizeFormula(value == null ? "" : value));
        cell.setCellStyle(style);
    }

    /**
     * Escapes stored text that spreadsheet applications may interpret as a formula
     * (spreadsheet/formula injection, CWE-1236): a stored value such as a worker name, room,
     * address or note beginning with =, +, - or @ can execute when an admin opens the workbook.
     * The leading {@code '} marks the cell as literal text and is how spreadsheet apps denote
     * exactly that. Genuine report values never begin with these characters, so the escape is
     * normally invisible.
     */
    private static String neutralizeFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }

    private static void autoSize(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.setColumnWidth(i, 18 * 256);
        }
    }

    // ---- Styles ----------------------------------------------------------------------------

    private static XSSFColor rgb(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new XSSFColor(new byte[] {(byte) r, (byte) g, (byte) b}, null);
    }

    private static CellStyle titleStyle(XSSFWorkbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 15);
        font.setColor(IndexedWhite());
        CellStyle s = wb.createCellStyle();
        s.setFont(font);
        s.setFillForegroundColor(rgb("0F3D5C"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static CellStyle subtitleStyle(XSSFWorkbook wb) {
        Font font = wb.createFont();
        font.setItalic(true);
        font.setColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_50_PERCENT.getIndex());
        CellStyle s = wb.createCellStyle();
        s.setFont(font);
        return s;
    }

    private static CellStyle headerStyle(XSSFWorkbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedWhite());
        CellStyle s = wb.createCellStyle();
        s.setFont(font);
        s.setFillForegroundColor(rgb("1C5E82"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        applyBorders(s);
        return s;
    }

    private static CellStyle bodyStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        applyBorders(s);
        return s;
    }

    private static CellStyle statusStyle(XSSFWorkbook wb, AssignmentStatus status) {
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(rgb(statusColorHex(status)));
        CellStyle s = wb.createCellStyle();
        s.setFont(font);
        applyBorders(s);
        return s;
    }

    private static String statusColorHex(AssignmentStatus status) {
        return switch (status) {
            case COMPLETED -> "008000";
            case CANCELLED -> "FF0000";
            case IN_PROGRESS, EN_ROUTE -> "FFA500";
            case SCHEDULED -> "808080";
        };
    }

    private static void applyBorders(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private static short IndexedWhite() {
        return org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex();
    }
}
