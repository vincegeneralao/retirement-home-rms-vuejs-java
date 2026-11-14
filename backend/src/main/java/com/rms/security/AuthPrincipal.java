package com.rms.security;

import java.util.List;

import com.rms.entity.Role;

/**
 * The authenticated caller resolved from a verified JWT. {@code roles} holds the JWT
 * {@code ROLE_*} authorities.
 */
public record AuthPrincipal(Long userId, String username, List<String> roles) {

    public boolean hasRole(Role role) {
        return roles != null && roles.contains("ROLE_" + role.name());
    }
}
