package com.rms.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Baseline security response headers, which Spring Security would otherwise provide (full
 * Spring Security is intentionally not used here). Applied before the request proceeds so the
 * headers are also present on short-circuited responses (e.g. a guard's 401/403).
 *
 * <p>API responses are JSON or XLSX and never render HTML, so they get a locked-down CSP
 * ({@code default-src 'none'}) and {@code no-store}. Swagger UI is the one HTML surface, so it
 * receives the looser policy its bundled assets need.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String API_CSP = "default-src 'none'; frame-ancestors 'none'";
    private static final String DOCS_CSP =
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean docs = isDocs(request.getRequestURI());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy", docs ? DOCS_CSP : API_CSP);
        if (!docs) {
            // API payloads are session-scoped; never let a browser or proxy cache them.
            response.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }

    private static boolean isDocs(String path) {
        return path.startsWith("/api/docs") || path.contains("swagger-ui");
    }
}
