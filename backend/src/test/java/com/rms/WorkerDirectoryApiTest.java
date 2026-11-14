package com.rms;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkerDirectoryApiTest extends AbstractApiTest {

    @Test
    void createsListsAndDeactivatesWorker() throws Exception {
        String admin = adminToken();
        String username = "w." + System.nanoTime();

        MvcResult created = mockMvc.perform(post("/api/admin/workers").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"username\":\"" + username + "\",\"fullName\":\"Test Worker\",\"password\":\"Worker#2026\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("WORKER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();
        long id = node(created).path("id").asLong();

        // Newly created worker can sign in.
        login(username, "Worker#2026");

        mockMvc.perform(get("/api/admin/workers").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(node(result).toString()).contains(username));

        mockMvc.perform(delete("/api/admin/workers/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        // Deactivated worker can no longer sign in.
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Worker#2026\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/workers").contentType(JSON).header("Authorization", bearer(admin))
                        .content("{\"username\":\"grace.loke\",\"fullName\":\"Dup\",\"password\":\"Worker#2026\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"))
                .andExpect(jsonPath("$.detail").value("Username \"grace.loke\" is already taken"));
    }

    @Test
    void invalidWorkerBodyReturnsFieldMap() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/workers").contentType(JSON).header("Authorization", bearer(admin))
                        .content("{\"username\":\"BAD USER\",\"fullName\":\"\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username")
                        .value("username may only contain lowercase letters, digits, dot, underscore or hyphen"))
                .andExpect(jsonPath("$.errors.fullName").value("fullName is required"))
                .andExpect(jsonPath("$.errors.password").value("password must be between 8 and 100 characters"));
    }

    @Test
    void createsAndListsClients() throws Exception {
        String admin = adminToken();
        MvcResult created = mockMvc.perform(post("/api/admin/clients").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"fullName\":\"New Resident\",\"roomNumber\":\"99Z\",\"address\":\"1 Test Street\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        JsonNode n = node(created);
        assertThat(n.path("location").asString()).contains("99Z").contains("1 Test Street");

        mockMvc.perform(get("/api/admin/clients").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(node(result).toString()).contains("New Resident"));
    }
}
