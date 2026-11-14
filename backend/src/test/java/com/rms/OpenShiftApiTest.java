package com.rms;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenShiftApiTest extends AbstractApiTest {

    private long firstClientId(String admin) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/admin/clients").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn();
        return node(res).path(0).path("id").asLong();
    }

    private long createWorker(String admin, String username) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/admin/workers").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"username\":\"" + username + "\",\"fullName\":\"OS Worker\",\"password\":\"Worker#2026\"}"))
                .andExpect(status().isCreated()).andReturn();
        return node(res).path("id").asLong();
    }

    private long createOpenShift(String admin, long clientId, String date, String start, String end) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/admin/open-shifts").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"clientId\":" + clientId + ",\"date\":\"" + date
                                + "\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\",\"notes\":\"seeded\"}"))
                .andExpect(status().isCreated()).andReturn();
        return node(res).path("id").asLong();
    }

    @Test
    void createAndListOpenShift() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        String date = LocalDate.now().plusDays(7).toString();
        long id = createOpenShift(admin, client, date, "09:00", "11:30");
        assertThat(id).isPositive();

        mockMvc.perform(get("/api/admin/open-shifts").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode arr = node(result);
                    boolean found = false;
                    for (JsonNode s : arr) {
                        if (s.path("id").asLong() == id) {
                            found = true;
                            assertThat(s.path("status").asString()).isEqualTo("OPEN");
                            assertThat(s.path("durationMinutes").asInt()).isEqualTo(150);
                        }
                    }
                    assertThat(found).isTrue();
                });
    }

    @Test
    void endTimeMustBeAfterStart() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        mockMvc.perform(post("/api/admin/open-shifts").contentType(JSON).header("Authorization", bearer(admin))
                        .content("{\"clientId\":" + client + ",\"date\":\"2030-01-01\",\"startTime\":\"11:00\",\"endTime\":\"11:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
    }

    @Test
    void unknownClientIsNotFound() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/open-shifts").contentType(JSON).header("Authorization", bearer(admin))
                        .content("{\"clientId\":987654,\"date\":\"2030-01-01\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void badDateFormatReturnsFieldMap() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        mockMvc.perform(post("/api/admin/open-shifts").contentType(JSON).header("Authorization", bearer(admin))
                        .content("{\"clientId\":" + client + ",\"date\":\"01/01/2030\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request body"))
                .andExpect(jsonPath("$.errors.date").value("date must be formatted as YYYY-MM-DD"));
    }

    @Test
    void claimSucceedsThenAlreadyTaken() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        String date = LocalDate.now().plusDays(3).toString();
        long shift = createOpenShift(admin, client, date, "05:00", "06:00");

        String worker = login("grace.loke", "Worker#2026");
        mockMvc.perform(post("/api/worker/open-shifts/" + shift + "/claim").header("Authorization", bearer(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TAKEN"))
                .andExpect(jsonPath("$.takenByName").value("Grace Loke"))
                .andExpect(jsonPath("$.assignmentId").isNumber());

        mockMvc.perform(post("/api/worker/open-shifts/" + shift + "/claim").header("Authorization", bearer(worker)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHIFT_ALREADY_TAKEN"));
    }

    @Test
    void claimOverlappingShiftIsRejected() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        String date = LocalDate.now().plusDays(9).toString();
        String username = "ov" + System.nanoTime();
        long worker = createWorker(admin, username);

        // Give the worker an existing shift 09:00–10:00 on the day.
        long a = createOpenShift(admin, client, date, "09:00", "10:00");
        mockMvc.perform(post("/api/admin/open-shifts/" + a + "/assign").contentType(JSON)
                        .header("Authorization", bearer(admin)).content("{\"workerId\":" + worker + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TAKEN"));

        // A second, overlapping slot (09:30–10:30) must not be claimable by the same worker.
        long b = createOpenShift(admin, client, date, "09:30", "10:30");
        String workerToken = login(username, "Worker#2026");
        mockMvc.perform(post("/api/worker/open-shifts/" + b + "/claim").header("Authorization", bearer(workerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHIFT_OVERLAP"));

        // And the aborted claim must have left slot B still open (transaction rolled back).
        mockMvc.perform(get("/api/admin/open-shifts").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    for (JsonNode s : node(result)) {
                        if (s.path("id").asLong() == b) {
                            assertThat(s.path("status").asString()).isEqualTo("OPEN");
                        }
                    }
                });
    }

    @Test
    void assignToInvalidWorkerFails() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        String date = LocalDate.now().plusDays(11).toString();
        long shift = createOpenShift(admin, client, date, "13:00", "14:00");

        mockMvc.perform(post("/api/admin/open-shifts/" + shift + "/assign").contentType(JSON)
                        .header("Authorization", bearer(admin)).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.detail").value("workerId is required"));

        mockMvc.perform(post("/api/admin/open-shifts/" + shift + "/assign").contentType(JSON)
                        .header("Authorization", bearer(admin)).content("{\"workerId\":999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WORKER"));
    }

    @Test
    void cancelOpenShift() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        String date = LocalDate.now().plusDays(13).toString();
        long shift = createOpenShift(admin, client, date, "07:00", "08:00");
        mockMvc.perform(delete("/api/admin/open-shifts/" + shift).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void claimUnknownShiftIsNotFound() throws Exception {
        String worker = login("grace.loke", "Worker#2026");
        mockMvc.perform(post("/api/worker/open-shifts/999999/claim").header("Authorization", bearer(worker)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OPEN_SHIFT_NOT_FOUND"));
    }
}
