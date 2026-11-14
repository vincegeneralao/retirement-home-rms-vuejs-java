package com.rms.security;

import java.util.List;

import com.rms.entity.Role;

/**
 * The authenticated caller resolved from a verified JWT. {@code roles} holds the JWT
 * {@code ROLE_*} authorities; {@code tokenVersion} mirrors the user row's counter so revoked
 * tokens can be rejected.
 */
public record AuthPrincipal(Long userId, String username, List<String> roles, int tokenVersion) {

    public boolean hasRole(Role role) {
        return roles != null && roles.contains("ROLE_" + role.name());
    }
}
