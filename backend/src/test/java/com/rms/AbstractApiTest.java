package com.rms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared Spring Boot + MockMvc harness. All API tests extend this so they reuse a single
 * application context. Provides login and JSON helpers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractApiTest {

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper json;

    /** Sign in and return the bearer token from the response. */
    protected String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return node(res).path("token").asString();
    }

    protected String adminToken() throws Exception {
        return login("admin", "Admin#2026");
    }

    protected JsonNode node(MvcResult res) throws Exception {
        return json.readTree(res.getResponse().getContentAsString());
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
