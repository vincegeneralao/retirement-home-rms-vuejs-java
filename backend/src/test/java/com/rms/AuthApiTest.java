package com.rms;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends AbstractApiTest {

    @Test
    void loginSuccessReturnsTokenAndProfile() throws Exception {
        String body = "{\"username\":\"Admin\",\"password\":\"Admin#2026\"}";
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        JsonNode n = node(res);
        assertThat(n.path("token").asString()).isNotBlank();
        assertThat(n.path("username").asString()).isEqualTo("admin");
        assertThat(n.path("expiresAt").asString()).endsWith("Z");
        assertThat(n.path("roles").toString()).contains("ROLE_ADMIN");
    }

    @Test
    void loginUnknownUserIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"whatever123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Request rejected"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void loginWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"admin\",\"password\":\"definitely-wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginValidationFailureReturnsFieldMap() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"\",\"password\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request body"))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username").value("username is required"))
                .andExpect(jsonPath("$.errors.password").value("password is required"));
    }

    @Test
    void loginAcceptsShortPasswordSinceBlankPasswordsAreLegitimate() throws Exception {
        // An admin reset leaves the worker on the empty password, so the sign-in gate must not
        // impose a length rule. Credential strength is enforced when a password is set instead.
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"admin\",\"password\":\"short\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
