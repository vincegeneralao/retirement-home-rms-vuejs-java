package com.rms.dto;

import java.util.List;

/** Response of POST /api/auth/login. */
public record AuthResponse(
        String token,
        String tokenType,
        String expiresAt,
        Long userId,
        String username,
        String fullName,
        List<String> roles
) {
}
