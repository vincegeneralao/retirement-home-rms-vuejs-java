package com.rms.service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rms.dto.AuthResponse;
import com.rms.dto.LoginRequest;
import com.rms.entity.User;
import com.rms.error.ApiErrors;
import com.rms.repository.UserRepository;
import com.rms.security.JwtService;

@Service
public class AuthService {

    /** A valid bcrypt digest used to burn an equivalent amount of time when the user is unknown. */
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final DateTimeFormatter ISO_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Verifies credentials and issues a signed token, or throws {@code INVALID_CREDENTIALS}.
     * Repeated failures for the same caller+username lock the attempt path with
     * {@code TOO_MANY_ATTEMPTS} (429).
     *
     * @param clientIp the caller's socket address (never a spoofable forwarded header)
     */
    public AuthResponse login(LoginRequest request, String clientIp) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String key = clientIp + "|" + username;
        rateLimiter.checkAllowed(key);

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            // Constant-ish work so a missing user is not detectably faster than a wrong password.
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            rateLimiter.recordFailure(key);
            throw ApiErrors.invalidCredentials();
        }
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(key);
            throw ApiErrors.invalidCredentials();
        }
        rateLimiter.recordSuccess(key);

        List<String> roles = List.of("ROLE_" + user.getRole().name());
        JwtService.Issued issued = jwtService.issue(user.getId(), user.getUsername(), user.getFullName(),
                roles, user.getTokenVersion());
        return new AuthResponse(
                issued.token(),
                "Bearer",
                ISO_MILLIS.format(issued.expiresAt()),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                roles,
                user.isPasswordResetRequired());
    }

    /**
     * Replaces the worker's own password after verifying the current one, clears the
     * reset-required flag and re-issues a token. The rotation invalidates the token the caller
     * is holding, so the replacement is handed back in the response to keep the session alive.
     */
    @Transactional
    public AuthResponse changePassword(Long userId, String currentPassword, String newPassword) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiErrors.invalidCurrentPassword();
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw ApiErrors.passwordUnchanged();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetRequired(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        return issueFor(user);
    }

    /**
     * Acknowledges the admin-issued blank password and keeps it. Weaker than choosing a real
     * password, so the UI warns about it, but the worker stays in control of their own account.
     * No token rotation happens because the credential itself did not change.
     */
    @Transactional
    public AuthResponse keepCurrentPassword(Long userId) {
        User user = requireUser(userId);
        if (!user.isPasswordResetRequired()) {
            throw ApiErrors.passwordResetNotPending();
        }
        user.setPasswordResetRequired(false);
        userRepository.save(user);
        return issueFor(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(ApiErrors::invalidCredentials);
    }

    private AuthResponse issueFor(User user) {
        List<String> roles = List.of("ROLE_" + user.getRole().name());
        JwtService.Issued issued = jwtService.issue(user.getId(), user.getUsername(), user.getFullName(),
                roles, user.getTokenVersion());
        return new AuthResponse(
                issued.token(),
                "Bearer",
                ISO_MILLIS.format(issued.expiresAt()),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                roles,
                user.isPasswordResetRequired());
    }
}
