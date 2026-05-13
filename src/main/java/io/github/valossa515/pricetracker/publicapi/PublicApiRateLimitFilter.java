package io.github.valossa515.pricetracker.publicapi;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-subject token-bucket rate limiter for the public API.
 * Applies only to {@code /api/v1/public/**}. Identifies the caller by JWT subject
 * (Cognito sub / client_id). Falls back to remote IP when no JWT is present.
 */
@Slf4j
@Component
@Order(20)
public class PublicApiRateLimitFilter extends OncePerRequestFilter {

    static final String PATH_PREFIX = "/api/v1/public/";

    private final PublicApiProperties properties;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public PublicApiRateLimitFilter(PublicApiProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = resolveKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limited\",\"message\":\"too many requests\"}");
            log.warn("Public API rate limit exceeded for key={}", key);
        }
    }

    private Bucket newBucket() {
        int rpm = properties.rateLimit().requestsPerMinute();
        int burst = properties.rateLimit().burst();
        Bandwidth limit = Bandwidth.builder()
                .capacity(burst)
                .refillIntervally(rpm, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return "sub:" + jwt.getSubject();
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return "ip:" + (comma > 0 ? ip.substring(0, comma).trim() : ip.trim());
        }
        return "ip:" + request.getRemoteAddr();
    }
}
