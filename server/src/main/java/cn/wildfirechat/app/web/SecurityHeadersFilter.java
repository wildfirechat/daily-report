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
    private static final String BASE_CSP_HEAD =
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; font-src 'self' data:";
    private static final String BASE_CSP_TAIL =
        "object-src 'none'; base-uri 'self'; form-action 'self'";

    @Value("${security.frame-ancestors:}")
    private String frameAncestors;

    /**
     * 桌面端（Electron）工作台通过本机 WebSocket（默认 ws://127.0.0.1:7983）与网页桥接，
     * 该连接受 CSP connect-src 约束，需要显式放行。只接受 ws:// / wss:// 来源，避免配置注入。
     */
    @Value("${security.connect-src-extra:}")
    private String connectSrcExtra;

    private String buildConnectSrc() {
        StringBuilder connectSrc = new StringBuilder("'self'");
        if (StringUtils.hasText(connectSrcExtra)) {
            for (String source : connectSrcExtra.trim().split("\\s+")) {
                if (source.startsWith("ws://") || source.startsWith("wss://")) {
                    connectSrc.append(' ').append(source);
                }
            }
        }
        return connectSrc.toString();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        StringBuilder csp = new StringBuilder(BASE_CSP_HEAD)
            .append("; connect-src ").append(buildConnectSrc())
            .append("; ").append(BASE_CSP_TAIL);
        if (StringUtils.hasText(frameAncestors)) {
            csp.append("; frame-ancestors ").append(frameAncestors);
            if ("'self'".equals(frameAncestors.trim())) {
                response.setHeader("X-Frame-Options", "SAMEORIGIN");
            }
        }
        response.setHeader("Content-Security-Policy", csp.toString());

        if (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        String path = request.getRequestURI();
        if (path != null && path.startsWith(request.getContextPath() + "/api/")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }

        // 页面外壳和启动自检脚本不能缓存：部署更新后仍使用旧 index.html 会引用已被删除的
        // 旧 hash 资源，客户端表现为白屏。带 hash 的 js/css 资源不受影响。
        if (path != null && (path.endsWith(".html") || path.endsWith("/")
            || path.endsWith("/js/boot-check.js"))) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
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
