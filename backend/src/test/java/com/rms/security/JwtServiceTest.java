package com.rms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.rms.config.AppProperties;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = "unit-test-signing-secret-must-be-at-least-32-bytes";

    private JwtService service() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(SECRET);
        return new JwtService(props);
    }

    @Test
    void issuesAndVerifiesRoundTrip() {
        JwtService svc = service();
        JwtService.Issued issued = svc.issue(42L, "grace.loke", "Grace Loke", List.of("ROLE_WORKER"), 3);

        AuthPrincipal me = svc.verify(issued.token());
        assertThat(me.userId()).isEqualTo(42L);
        assertThat(me.username()).isEqualTo("grace.loke");
        assertThat(me.roles()).containsExactly("ROLE_WORKER");
        assertThat(me.tokenVersion()).isEqualTo(3);
        assertThat(me.hasRole(com.rms.entity.Role.WORKER)).isTrue();
        assertThat(me.hasRole(com.rms.entity.Role.ADMIN)).isFalse();
    }

    @Test
    void legacyTokenWithoutVersionClaimVerifiesAsZero() {
        JwtService svc = service();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String legacy = Jwts.builder()
                .issuer("retirement-home-rms")
                .subject("old.user")
                .claim("uid", 7L)
                .claim("roles", List.of("ROLE_WORKER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(svc.verify(legacy).tokenVersion()).isZero();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService svc = service();
        String token = svc.issue(1L, "a", "A", List.of("ROLE_ADMIN"), 0).token();
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThatThrownBy(() -> svc.verify(tampered)).isInstanceOf(JwtException.class);
    }
}
