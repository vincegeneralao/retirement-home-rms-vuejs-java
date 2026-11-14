package com.rms.service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Verifies credentials and issues a signed token, or throws {@code INVALID_CREDENTIALS}. */
    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            // Constant-ish work so a missing user is not detectably faster than a wrong password.
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw ApiErrors.invalidCredentials();
        }
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiErrors.invalidCredentials();
        }

        List<String> roles = List.of("ROLE_" + user.getRole().name());
        JwtService.Issued issued = jwtService.issue(user.getId(), user.getUsername(), user.getFullName(), roles);
        return new AuthResponse(
                issued.token(),
                "Bearer",
                ISO_MILLIS.format(issued.expiresAt()),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                roles);
    }
}
