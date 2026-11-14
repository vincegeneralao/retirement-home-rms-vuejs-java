package com.rms;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TokenRevocationApiTest extends AbstractApiTest {

    @Test
    void deactivatedWorkersTokenStopsWorkingImmediately() throws Exception {
        String admin = adminToken();
        String username = "revoked." + System.nanoTime();

        MvcResult created = mockMvc.perform(post("/api/admin/workers").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"username\":\"" + username + "\",\"fullName\":\"Doomed Worker\","
                                + "\"password\":\"Worker#2026\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long id = node(created).path("id").asLong();

        String worker = login(username, "Worker#2026");
        mockMvc.perform(get("/api/worker/assignments/upcoming").header("Authorization", bearer(worker)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/workers/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        // The same token — cryptographically still valid — is now rejected.
        mockMvc.perform(get("/api/worker/assignments/upcoming").header("Authorization", bearer(worker)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.detail").value("Your session is no longer valid. Sign in again."));

        mockMvc.perform(get("/api/admin/clients").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()); // admin sessions keep working
    }
}
