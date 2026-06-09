package com.docqueue.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Distributed IP-based rate limiting filter using Redis.
 *
 * Rules:
 * - Auth endpoints:        5 requests / minute  (brute-force protection)
 * - AI endpoints:          5 requests / minute  (cost protection)
 * - Booking endpoints:     10 requests / minute (spam prevention)
 * - Doctor Search:         15 requests / minute (anti-scraping)
 * - Other API endpoints:   60 requests / minute (general throttle)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        String ip  = resolveClientIp(request);
        String uri = request.getRequestURI();

        String type;
        int limit;

        if (uri.startsWith("/api/v1/auth/login") || uri.startsWith("/api/v1/auth/register")) {
            type = "auth";
            limit = 5;
        } else if (uri.startsWith("/api/v1/ai")) {
            type = "ai";
            limit = 5;
        } else if (uri.startsWith("/api/v1/doctors") && "GET".equals(request.getMethod())) {
            type = "search";
            limit = 15;
        } else if (uri.startsWith("/api/v1/appointments") && "POST".equals(request.getMethod())) {
            type = "booking";
            limit = 10;
        } else {
            type = "general";
            limit = 60;
        }

        if (isAllowed(type, ip, limit)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on {} (Type: {}, Limit: {})", ip, uri, type, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"success\":false,\"error\":\"Too many requests. Please slow down and try again.\"}"
            );
        }
    }

    private boolean isAllowed(String type, String ip, int limit) {
        String key = "ratelimit:" + type + ":" + ip;
        Long current = redisTemplate.opsForValue().increment(key);
        
        if (current != null && current == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        
        return current != null && current <= limit;
    }

    private String resolveClientIp(HttpServletRequest request) {
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
