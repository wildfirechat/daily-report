package cn.wildfirechat.app.shiro;

import cn.wildfirechat.app.RestResult;
import com.google.gson.Gson;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Double-submit-cookie CSRF protection for cookie-authenticated requests.
 *
 * <p>Requests authenticated with the explicit {@code authToken} header are not vulnerable to CSRF
 * and are therefore exempt. Login endpoints are exempt because no session exists yet. All other
 * state-changing API requests must echo the XSRF cookie in the X-XSRF-TOKEN header.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class CsrfFilter extends OncePerRequestFilter {
    private static final String COOKIE_NAME = "XSRF-TOKEN";
    private static final String HEADER_NAME = "X-XSRF-TOKEN";
    private static final Set<String> SAFE_METHODS =
        new HashSet<>(Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE"));
    private static final Set<String> LOGIN_PATHS =
        new HashSet<>(Arrays.asList("/api/login", "/api/user/login"));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = readCookie(request, COOKIE_NAME);
        if (token == null || token.isEmpty()) {
            token = UUID.randomUUID().toString();
            StringBuilder cookie = new StringBuilder();
            cookie.append(COOKIE_NAME).append('=').append(token)
                .append("; Path=/; SameSite=Lax");
            if (request.isSecure()) {
                cookie.append("; Secure");
            }
            response.addHeader("Set-Cookie", cookie.toString());
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (SAFE_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))
            || LOGIN_PATHS.contains(path)
            || request.getHeader("authToken") != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String submitted = request.getHeader(HEADER_NAME);
        if (submitted == null || !MessageDigest.isEqual(
            submitted.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write(new Gson().toJson(
                RestResult.error(RestResult.RestCode.ERROR_CSRF_TOKEN)));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
