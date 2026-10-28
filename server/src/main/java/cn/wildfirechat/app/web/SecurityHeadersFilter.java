package cn.wildfirechat.app.web;

import cn.wildfirechat.app.RestResult;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Baseline browser security headers and a small request-size guard for JSON API calls.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {
    private static final long MAX_API_BODY_BYTES = 64 * 1024;
    private static final String BASE_CSP =
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'";

    @Value("${security.frame-ancestors:}")
    private String frameAncestors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        String csp = BASE_CSP;
        if (StringUtils.hasText(frameAncestors)) {
            csp += "; frame-ancestors " + frameAncestors;
            if ("'self'".equals(frameAncestors.trim())) {
                response.setHeader("X-Frame-Options", "SAMEORIGIN");
            }
        }
        response.setHeader("Content-Security-Policy", csp);

        if (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        String path = request.getRequestURI();
        if (path != null && path.startsWith(request.getContextPath() + "/api/")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }

        if (path != null && path.startsWith(request.getContextPath() + "/api/")
            && request.getContentLengthLong() > MAX_API_BODY_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(
                RestResult.error(RestResult.RestCode.ERROR_REQUEST_TOO_LARGE)));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
