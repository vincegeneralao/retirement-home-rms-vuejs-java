package com.rms.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads a bearer token (if present) from the Authorization header, verifies it and stores the
 * resolved {@link AuthPrincipal} as a request attribute. It never rejects a request on its own —
 * authentication is optional here; the {@link RoleInterceptor} decides which endpoints require
 * which role.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_ATTRIBUTE = "rms.auth";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    request.setAttribute(AUTH_ATTRIBUTE, jwtService.verify(token));
                } catch (JwtException | IllegalArgumentException ex) {
                    // Invalid/expired token: leave the request unauthenticated.
                    request.removeAttribute(AUTH_ATTRIBUTE);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
