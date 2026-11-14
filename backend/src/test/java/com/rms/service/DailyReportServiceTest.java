package com.rms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.rms.entity.Assignment;
import com.rms.entity.AssignmentStatus;
import com.rms.repository.AssignmentRepository;

class DailyReportServiceTest {

    private Assignment row(long id, AssignmentStatus status, String notes) {
        return Assignment.builder()
                .id(id).workerId(1L).clientId(2L)
                .scheduledStart(LocalDateTime.of(2026, 3, 5, 9, 0))
                .durationMinutes(60).status(status).notes(notes).build();
    }

    @Test
    void rendersEveryAssignmentStatus() {
        AssignmentRepository repo = mock(AssignmentRepository.class);
        when(repo.findInRange(any(), any())).thenReturn(List.of(
                row(1L, AssignmentStatus.SCHEDULED, null),
                row(2L, AssignmentStatus.IN_PROGRESS, "kick off"),
                row(3L, AssignmentStatus.EN_ROUTE, null),
                row(4L, AssignmentStatus.COMPLETED, "done"),
                row(5L, AssignmentStatus.CANCELLED, "off")));

        byte[] xlsx = new DailyReportService(repo).generate(LocalDate.of(2026, 3, 5));
        assertThat(xlsx).isNotEmpty();
    }

    @Test
    void rendersEmptyDay() {
        AssignmentRepository repo = mock(AssignmentRepository.class);
        when(repo.findInRange(any(), any())).thenReturn(List.of());
        assertThat(new DailyReportService(repo).generate(LocalDate.of(2026, 3, 6))).isNotEmpty();
    }

    @Test
    void formulaLikeStoredValuesAreNeutralized() throws Exception {
        AssignmentRepository repo = mock(AssignmentRepository.class);
        when(repo.findInRange(any(), any())).thenReturn(List.of(
                row(1L, AssignmentStatus.SCHEDULED, "=HYPERLINK(\"http://evil\")"),
                row(2L, AssignmentStatus.COMPLETED, "Regular note")));

        byte[] xlsx = new DailyReportService(repo).generate(LocalDate.of(2026, 3, 5));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = wb.getSheet("Daily Schedule");
            assertThat(sheet.getRow(3).getCell(7).getStringCellValue())
                    .isEqualTo("'=HYPERLINK(\"http://evil\")");
            assertThat(sheet.getRow(4).getCell(7).getStringCellValue()).isEqualTo("Regular note");
        }
    }
}
