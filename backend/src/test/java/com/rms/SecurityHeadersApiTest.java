package com.rms;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityHeadersApiTest extends AbstractApiTest {

    @Test
    void shortCircuitedResponsesStillCarryBaselineHeaders() throws Exception {
        mockMvc.perform(get("/api/admin/workers")) // 401 from the guard, before any handler
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void successfulApiResponseCarriesNoStore() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin#2026\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void swaggerUiGetsTheDocsPolicyNotTheLockedDownOne() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("unsafe-inline")));
    }
}
