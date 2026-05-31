package com.docqueue.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiting filter using Bucket4j token bucket algorithm.
 *
 * Rules:
 * - Auth endpoints:        5 requests / minute  (brute-force protection)
 * - Booking endpoints:     10 requests / minute (spam prevention)
 * - Other API endpoints:   60 requests / minute (general throttle)
 *
 * Buckets are stored in a ConcurrentHashMap per IP.
 * Production upgrade: use Bucket4j + Redis for distributed rate limiting.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // IP → Bucket map (in-memory; replace with Redis for multi-instance)
    private final Map<String, Bucket> authBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> bookingBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        String ip  = resolveClientIp(request);
        String uri = request.getRequestURI();

        Bucket bucket;

        if (uri.startsWith("/api/v1/auth/login") || uri.startsWith("/api/v1/auth/register")) {
            bucket = authBuckets.computeIfAbsent(ip, k -> buildBucket(5, Duration.ofMinutes(1)));
        } else if (uri.startsWith("/api/v1/appointments") && "POST".equals(request.getMethod())) {
            bucket = bookingBuckets.computeIfAbsent(ip, k -> buildBucket(10, Duration.ofMinutes(1)));
        } else {
            bucket = generalBuckets.computeIfAbsent(ip, k -> buildBucket(60, Duration.ofMinutes(1)));
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on {}", ip, uri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {"success":false,"error":"Too many requests. Please slow down and try again."}
                """);
        }
    }

    private Bucket buildBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, period));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Respect Cloudflare / Nginx X-Forwarded-For
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp;
        }
        return request.getRemoteAddr();
    }
}
