package com.rms.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.rms.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Issues and verifies HS256 JWTs carrying the caller's identity and role authorities. */
@Service
public class JwtService {

    private final AppProperties props;
    private final SecretKey key;

    public JwtService(AppProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** An issued token together with its absolute expiry. */
    public record Issued(String token, Instant expiresAt) {
    }

    /** Mint a signed token for a user, returning it plus the expiry instant. */
    public Issued issue(Long userId, String username, String fullName, List<String> roles, int tokenVersion) {
        Instant now = Instant.now();
        Instant expiry = now.plus(props.getJwt().getTtl());
        String token = Jwts.builder()
                .issuer(props.getJwt().getIssuer())
                .subject(username)
                .claim("uid", userId)
                .claim("name", fullName)
                .claim("roles", roles)
                .claim("ver", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new Issued(token, expiry);
    }

    /**
     * Verify and decode a token into a principal.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, tampered with or expired
     */
    @SuppressWarnings("unchecked")
    public AuthPrincipal verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        List<String> roles = claims.get("roles", List.class);
        Long uid = claims.get("uid", Number.class).longValue();
        Number ver = claims.get("ver", Number.class);
        return new AuthPrincipal(uid, claims.getSubject(), roles == null ? List.of() : roles,
                ver == null ? 0 : ver.intValue()); // tokens minted before versioning stay valid at 0
    }
}
