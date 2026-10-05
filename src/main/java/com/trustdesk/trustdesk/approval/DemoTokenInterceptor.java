package com.trustdesk.trustdesk.approval;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DemoTokenInterceptor implements HandlerInterceptor {
    private final String expectedToken;

    public DemoTokenInterceptor(
            @Value("${trustdesk.demo-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        if (expectedToken.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Demo token is not configured");
            return false;
        }

        String providedToken = request.getHeader("X-Demo-Token");
        if (providedToken == null
                || !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Valid X-Demo-Token header is required");
            return false;
        }

        return true;
    }
}