package com.rms.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The encoder must verify the blank secret an administrator hands out on a password reset,
 * which stock {@code BCryptPasswordEncoder.matches} refuses to do.
 */
class SecurityBeansTest {

    private final PasswordEncoder encoder = new SecurityBeans().passwordEncoder();

    @Test
    void blankSecretRoundTrips() {
        String hash = encoder.encode("");
        assertThat(hash).hasSize(60);
        assertThat(encoder.matches("", hash)).isTrue();
        assertThat(encoder.matches("somebody-elses-secret", hash)).isFalse();
    }

    @Test
    void ordinarySecretsStillWork() {
        String hash = encoder.encode("Worker#2026");
        assertThat(encoder.matches("Worker#2026", hash)).isTrue();
        assertThat(encoder.matches("Worker#2027", hash)).isFalse();
        assertThat(encoder.matches("", hash)).isFalse();
    }

    @Test
    void missingOrMalformedValuesNeverMatch() {
        String hash = encoder.encode("");
        assertThat(encoder.matches(null, hash)).isFalse();
        assertThat(encoder.matches("", null)).isFalse();
        assertThat(encoder.matches("", "")).isFalse();
        assertThat(encoder.matches("", "not-a-bcrypt-digest")).isFalse();
    }
}
