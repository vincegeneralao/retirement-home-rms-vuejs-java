package com.rms;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityGuardTest extends AbstractApiTest {

    @Test
    void adminEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/workers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail")
                        .value("Provide a valid 'Authorization: Bearer <token>' header."));
    }

    @Test
    void invalidTokenIsTreatedAsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/workers").header("Authorization", bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    @Test
    void workerCannotCallAdminEndpoint() throws Exception {
        String worker = login("grace.loke", "Worker#2026");
        mockMvc.perform(get("/api/admin/workers").header("Authorization", bearer(worker)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Your role is not allowed to call this endpoint."));
    }

    @Test
    void adminCannotCallWorkerEndpoint() throws Exception {
        mockMvc.perform(get("/api/worker/assignments/upcoming").header("Authorization", bearer(adminToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    void workerCanCallWorkerEndpoint() throws Exception {
        String worker = login("grace.loke", "Worker#2026");
        mockMvc.perform(get("/api/worker/assignments/upcoming").header("Authorization", bearer(worker)))
                .andExpect(status().isOk());
    }
}
