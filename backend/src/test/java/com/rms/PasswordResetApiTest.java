package com.rms;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin-initiated password reset to the blank default, and the worker's two ways out of it:
 * choose a new password, or keep the blank one after being warned.
 */
class PasswordResetApiTest extends AbstractApiTest {

    /** Creates a throwaway worker and returns its username and id. */
    private long createWorker(String admin, String username) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/admin/workers").contentType(JSON)
                        .header("Authorization", bearer(admin))
                        .content("{\"username\":\"" + username + "\",\"fullName\":\"Reset Target\","
                                + "\"password\":\"Worker#2026\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return node(created).path("id").asLong();
    }

    private JsonNode loginNode(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return node(res);
    }

    @Test
    void resetLetsWorkerSignInWithBlankPasswordAndFlagsThem() throws Exception {
        String admin = adminToken();
        String username = "reset." + System.nanoTime();
        long id = createWorker(admin, username);

        mockMvc.perform(post("/api/admin/workers/" + id + "/reset-password")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        // The old password no longer works.
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Worker#2026\"}"))
                .andExpect(status().isUnauthorized());

        JsonNode n = loginNode(username, "");
        assertThat(n.path("mustChangePassword").asBoolean()).isTrue();
        assertThat(n.path("token").asString()).isNotBlank();
    }

    @Test
    void resetKillsTheWorkersInFlightToken() throws Exception {
        String admin = adminToken();
        String username = "rotated." + System.nanoTime();
        long id = createWorker(admin, username);

        String worker = login(username, "Worker#2026");
        mockMvc.perform(get("/api/worker/assignments/upcoming").header("Authorization", bearer(worker)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/workers/" + id + "/reset-password")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/worker/assignments/upcoming").header("Authorization", bearer(worker)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Your session is no longer valid. Sign in again."));
    }

    @Test
    void resetRejectsUnknownIdAndNonAdmin() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/workers/999999/reset-password").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKER_NOT_FOUND"));

        String worker = login("grace.loke", "Worker#2026");
        mockMvc.perform(post("/api/admin/workers/1/reset-password").header("Authorization", bearer(worker)))
                .andExpect(status().isForbidden());
    }

    @Test
    void changingPasswordClearsTheFlagAndRetiresTheBlankSecret() throws Exception {
        String admin = adminToken();
        String username = "changed." + System.nanoTime();
        long id = createWorker(admin, username);
        mockMvc.perform(post("/api/admin/workers/" + id + "/reset-password")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        String token = loginNode(username, "").path("token").asString();

        mockMvc.perform(post("/api/worker/password/change").contentType(JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"currentPassword\":\"\",\"newPassword\":\"Sunny#Day1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // The blank password is dead, the new one works and no longer nags.
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"\"}"))
                .andExpect(status().isUnauthorized());

        JsonNode again = loginNode(username, "Sunny#Day1");
        assertThat(again.path("mustChangePassword").asBoolean()).isFalse();
    }

    @Test
    void changePasswordRejectsWrongCurrentPasswordAndWeakNewPassword() throws Exception {
        String token = login("grace.loke", "Worker#2026");

        mockMvc.perform(post("/api/worker/password/change").contentType(JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"currentPassword\":\"not-it\",\"newPassword\":\"Sunny#Day1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        mockMvc.perform(post("/api/worker/password/change").contentType(JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"currentPassword\":\"Worker#2026\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword")
                        .value("newPassword must be between 6 and 100 characters"));
    }

    @Test
    void keepLeavesTheBlankPasswordUsableAndStopsTheNagging() throws Exception {
        String admin = adminToken();
        String username = "keeper." + System.nanoTime();
        long id = createWorker(admin, username);
        mockMvc.perform(post("/api/admin/workers/" + id + "/reset-password")
                .header("Authorization", bearer(admin)));

        String token = loginNode(username, "").path("token").asString();

        mockMvc.perform(post("/api/worker/password/keep").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        JsonNode again = loginNode(username, "");
        assertThat(again.path("mustChangePassword").asBoolean()).isFalse();
    }

    @Test
    void keepIsRejectedWhenNoResetIsPending() throws Exception {
        String token = login("grace.loke", "Worker#2026");
        mockMvc.perform(post("/api/worker/password/keep").header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PASSWORD_RESET_NOT_PENDING"));
    }

    @Test
    void passwordEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/worker/password/keep"))
                .andExpect(status().isUnauthorized());
    }
}
