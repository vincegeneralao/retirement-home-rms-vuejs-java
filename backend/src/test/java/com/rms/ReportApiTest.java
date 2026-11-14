package com.rms;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportApiTest extends AbstractApiTest {

    @Test
    void downloadsDailyWorkbook() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/admin/reports/daily/download")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString("spreadsheetml.sheet")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();

        String cd = res.getResponse().getHeader("Content-Disposition");
        assertThat(cd).contains("attachment").contains("daily-schedule-").contains("filename*=UTF-8''");

        byte[] bytes = res.getResponse().getContentAsByteArray();
        assertThat(bytes.length).isGreaterThan(1000);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Daily Schedule");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).startsWith("Daily Schedule");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Time");
        }
    }

    @Test
    void acceptsDateParameter() throws Exception {
        mockMvc.perform(get("/api/admin/reports/daily/download")
                        .param("date", LocalDate.now().plusDays(2).toString())
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsBadDate() throws Exception {
        mockMvc.perform(get("/api/admin/reports/daily/download")
                        .param("date", "nope")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }
}
