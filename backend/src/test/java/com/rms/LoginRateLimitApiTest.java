package com.rms;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginRateLimitApiTest extends AbstractApiTest {

    private static final int DEFAULT_ALLOWANCE = 5; // rms.rate-limit.max-failed-attempts default

    @Test
    void repeatedFailuresLockSignInForThatUsername() throws Exception {
        // Dedicated username so the lockout cannot affect any other test.
        String username = "bruteforce." + System.nanoTime();
        for (int i = 0; i < DEFAULT_ALLOWANCE; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"))
                .andExpect(jsonPath("$.detail").value("Too many failed sign-in attempts. Try again later."))
                .andExpect(header().exists("Retry-After"));

        // Another account's sign-ins are unaffected.
        adminToken();
    }

    @Test
    void lockedOutKeyIsStillRateLimitedNotReauthenticated() throws Exception {
        String username = "bruteforce2." + System.nanoTime();
        for (int i = 0; i < DEFAULT_ALLOWANCE; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized());
        }
        // Even a correct password for the locked-out key is refused until the lockout passes.
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Admin#2026\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", containsString("9")));
    }
}
