package com.rms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.rms.config.AppProperties;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private JwtService service() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("unit-test-signing-secret-must-be-at-least-32-bytes");
        return new JwtService(props);
    }

    @Test
    void issuesAndVerifiesRoundTrip() {
        JwtService svc = service();
        JwtService.Issued issued = svc.issue(42L, "grace.loke", "Grace Loke", List.of("ROLE_WORKER"));

        AuthPrincipal me = svc.verify(issued.token());
        assertThat(me.userId()).isEqualTo(42L);
        assertThat(me.username()).isEqualTo("grace.loke");
        assertThat(me.roles()).containsExactly("ROLE_WORKER");
        assertThat(me.hasRole(com.rms.entity.Role.WORKER)).isTrue();
        assertThat(me.hasRole(com.rms.entity.Role.ADMIN)).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService svc = service();
        String token = svc.issue(1L, "a", "A", List.of("ROLE_ADMIN")).token();
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThatThrownBy(() -> svc.verify(tampered)).isInstanceOf(JwtException.class);
    }
}
