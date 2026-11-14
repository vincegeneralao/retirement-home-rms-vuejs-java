package com.rms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rms.entity.User;
import com.rms.repository.UserRepository;
import com.rms.security.AuthPrincipal;

/**
 * Answers whether a verified JWT should still be honored. Tokens are otherwise valid until
 * their expiry; this consults the user row on each guarded request so deactivations,
 * credential rotations ({@code tokenVersion} bumps) and role changes take effect at once.
 */
@Service
public class TokenRevocationService {

    private final UserRepository userRepository;

    public TokenRevocationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** {@code false} once the user is gone, disabled, rotated out, or no longer holds the token's role. */
    @Transactional(readOnly = true)
    public boolean isLive(AuthPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            return false;
        }
        User user = userRepository.findById(principal.userId()).orElse(null);
        if (user == null || !user.isEnabled()) {
            return false;
        }
        if (user.getTokenVersion() != principal.tokenVersion()) {
            return false;
        }
        return principal.roles().contains("ROLE_" + user.getRole().name());
    }
}
