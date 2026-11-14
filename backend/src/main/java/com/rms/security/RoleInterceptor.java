package com.rms.security;

import java.io.IOException;

import org.springframework.web.servlet.HandlerInterceptor;

import com.rms.entity.Role;
import com.rms.service.TokenRevocationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Guards a path group behind a single role. Absent/invalid credentials yield the API's 401 body;
 * a valid caller with the wrong role yields the 403 body — both byte-for-byte matching the
 * reference backend. A token that is cryptographically valid but no longer honored (the worker
 * was deactivated, their credentials rotated or their role changed) also yields a 401, via
 * {@link TokenRevocationService}.
 */
public class RoleInterceptor implements HandlerInterceptor {

    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;

    private static final String UNAUTHORIZED_BODY =
            "{\"type\":\"about:blank\",\"title\":\"Authentication required\",\"status\":401,"
            + "\"detail\":\"Provide a valid 'Authorization: Bearer <token>' header.\"}";

    private static final String FORBIDDEN_BODY =
            "{\"type\":\"about:blank\",\"title\":\"Access denied\",\"status\":403,"
            + "\"detail\":\"Your role is not allowed to call this endpoint.\"}";

    private static final String REVOKED_BODY =
            "{\"type\":\"about:blank\",\"title\":\"Authentication required\",\"status\":401,"
            + "\"detail\":\"Your session is no longer valid. Sign in again.\"}";

    private final Role requiredRole;
    private final TokenRevocationService revocations;

    public RoleInterceptor(Role requiredRole, TokenRevocationService revocations) {
        this.requiredRole = requiredRole;
        this.revocations = revocations;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Object attr = request.getAttribute(JwtAuthFilter.AUTH_ATTRIBUTE);
        if (!(attr instanceof AuthPrincipal principal)) {
            write(response, UNAUTHORIZED, UNAUTHORIZED_BODY);
            return false;
        }
        if (!principal.hasRole(requiredRole)) {
            write(response, FORBIDDEN, FORBIDDEN_BODY);
            return false;
        }
        if (!revocations.isLive(principal)) {
            write(response, UNAUTHORIZED, REVOKED_BODY);
            return false;
        }
        return true;
    }

    private void write(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }
}
