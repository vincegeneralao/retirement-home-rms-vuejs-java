package com.rms;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityApiTest extends AbstractApiTest {

    private long createWorker(String admin) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/admin/workers").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"username\":\"w." + System.nanoTime() + "\",\"fullName\":\"Avail Worker\",\"password\":\"Worker#2026\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return node(res).path("id").asLong();
    }

    @Test
    void addListDeleteAvailability() throws Exception {
        String admin = adminToken();
        long w = createWorker(admin);

        MvcResult added = mockMvc.perform(post("/api/admin/workers/" + w + "/availability").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"08:00\",\"endTime\":\"12:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andReturn();
        long availId = node(added).path("id").asLong();

        mockMvc.perform(get("/api/admin/workers/" + w + "/availability").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").value("08:00"));

        mockMvc.perform(delete("/api/admin/workers/" + w + "/availability/" + availId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void overlappingWindowIsRejected() throws Exception {
        String admin = adminToken();
        long w = createWorker(admin);
        mockMvc.perform(post("/api/admin/workers/" + w + "/availability").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"08:00\",\"endTime\":\"12:00\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/workers/" + w + "/availability").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"11:00\",\"endTime\":\"13:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AVAILABILITY_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("MONDAY 11:00-13:00 overlaps existing availability 08:00-12:00"));
    }

    @Test
    void windowMustEndAfterItStarts() throws Exception {
        String admin = adminToken();
        long w = createWorker(admin);
        mockMvc.perform(post("/api/admin/workers/" + w + "/availability").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"dayOfWeek\":\"TUESDAY\",\"startTime\":\"12:00\",\"endTime\":\"12:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AVAILABILITY_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("TUESDAY 12:00-12:00 must end after it starts"));
    }

    @Test
    void availabilityForUnknownWorkerIsNotFound() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/workers/999999/availability").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"08:00\",\"endTime\":\"12:00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKER_NOT_FOUND"));
    }
}
