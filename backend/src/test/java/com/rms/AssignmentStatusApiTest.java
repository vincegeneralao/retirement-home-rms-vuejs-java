package com.rms;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssignmentStatusApiTest extends AbstractApiTest {

    private long firstClientId(String admin) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/admin/clients").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn();
        return node(res).path(0).path("id").asLong();
    }

    private long createWorker(String admin, String username) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/admin/workers").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"username\":\"" + username + "\",\"fullName\":\"Status Worker\",\"password\":\"Worker#2026\"}"))
                .andExpect(status().isCreated()).andReturn();
        return node(res).path("id").asLong();
    }

    /** Publish + assign a shift to {@code worker}, returning the created assignment id. */
    private long assignShift(String admin, long client, long worker, String date, String start, String end)
            throws Exception {
        MvcResult os = mockMvc.perform(post("/api/admin/open-shifts").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"clientId\":" + client + ",\"date\":\"" + date
                                + "\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        long shift = node(os).path("id").asLong();
        MvcResult as = mockMvc.perform(post("/api/admin/open-shifts/" + shift + "/assign").contentType(JSON)
                        .header("Authorization", bearer(admin)).content("{\"workerId\":" + worker + "}"))
                .andExpect(status().isOk()).andReturn();
        return node(as).path("assignmentId").asLong();
    }

    @Test
    void workerUpdatesOwnStatusAndSeesIt() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        String date = LocalDate.now().plusDays(4).toString();
        String username = "st" + System.nanoTime();
        long worker = createWorker(admin, username);
        long assignmentId = assignShift(admin, client, worker, date, "09:00", "10:00");

        String token = login(username, "Worker#2026");
        mockMvc.perform(patch("/api/worker/assignments/" + assignmentId + "/status").contentType(JSON)
                        .header("Authorization", bearer(token)).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/worker/assignments/today").param("date", date).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(node(result).toString()).contains("IN_PROGRESS"));

        mockMvc.perform(get("/api/worker/assignments/upcoming").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void invalidStatusIsRejected() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        String date = LocalDate.now().plusDays(4).toString();
        String username = "st" + System.nanoTime();
        long worker = createWorker(admin, username);
        long assignmentId = assignShift(admin, client, worker, date, "09:00", "10:00");

        String token = login(username, "Worker#2026");
        mockMvc.perform(patch("/api/worker/assignments/" + assignmentId + "/status").contentType(JSON)
                        .header("Authorization", bearer(token)).content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.detail").value("status must be one of: SCHEDULED, IN_PROGRESS, COMPLETED"));
    }

    @Test
    void cannotUpdateAnotherWorkersShift() throws Exception {
        String admin = adminToken();
        long client = firstClientId(admin);
        String date = LocalDate.now().plusDays(4).toString();
        long owner = createWorker(admin, "own" + System.nanoTime());
        long assignmentId = assignShift(admin, client, owner, date, "09:00", "10:00");

        String intruder = login("priya.raman", "Worker#2026");
        mockMvc.perform(patch("/api/worker/assignments/" + assignmentId + "/status").contentType(JSON)
                        .header("Authorization", bearer(intruder)).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_YOUR_ASSIGNMENT"));
    }
}
