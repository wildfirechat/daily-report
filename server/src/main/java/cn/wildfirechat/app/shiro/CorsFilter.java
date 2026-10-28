package cn.wildfirechat.app.shiro;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Strict CORS policy.
 *
 * <p>Only explicitly configured origins (plus the application's own origin) receive CORS headers.
 * Credentials are never combined with a wildcard or a reflected arbitrary origin.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CorsFilter extends OncePerRequestFilter {
    private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS =
        "Content-Type, Accept, X-Requested-With, authToken, X-XSRF-TOKEN";

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsConfig;

    private Set<String> allowedOrigins = Collections.emptySet();

    @PostConstruct
    private void initAllowedOrigins() {
        if (!StringUtils.hasText(allowedOriginsConfig)) {
            return;
        }
        Set<String> origins = new LinkedHashSet<>();
        Arrays.stream(allowedOriginsConfig.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .filter(origin -> !"*".equals(origin))
            .forEach(origins::add);
        this.allowedOrigins = origins;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin) && isAllowed(origin, request)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
            response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
            response.setHeader("Access-Control-Expose-Headers", "authToken");
            response.setHeader("Access-Control-Max-Age", "600");
            response.addHeader("Vary", "Origin");
        } else if (StringUtils.hasText(origin) && "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // Reject a preflight for an origin that is not explicitly allowed.
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String origin, HttpServletRequest request) {
        if (allowedOrigins.contains(origin)) {
            return true;
        }
        return origin.equals(selfOrigin(request));
    }

    private String selfOrigin(HttpServletRequest request) {
        StringBuilder origin = new StringBuilder();
        origin.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
            || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
        if (!defaultPort) {
            origin.append(':').append(port);
        }
        return origin.toString();
    }
}
