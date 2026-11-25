package com.example.Blogging_Platform_IV.handler;

import com.example.Blogging_Platform_IV.controller.rest.ApiResponse;
import com.example.Blogging_Platform_IV.filter.JwtAuthenticationFilter;
import com.example.Blogging_Platform_IV.service.SecurityEvents;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Writes the two security errors in the same JSON shape as every other REST response:
 * 401 when nobody is logged in (no token, bad token, expired token),
 * 403 when somebody is logged in but their role is not allowed.
 */
@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;
    private final SecurityEvents events;

    public JsonSecurityHandlers(ObjectMapper mapper, SecurityEvents events) {
        this.mapper = mapper;
        this.events = events;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR);
        // A rejected token was already logged by the filter. Only a request with no token is logged here.
        if (reason == null) {
            events.unauthenticatedRequest(request.getRemoteAddr(), request.getRequestURI());
        }
        String message = reason != null ? reason.toString() : "Log in first: send a Bearer token.";
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, message);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        events.accessDenied(request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "unknown",
                request.getRequestURI());
        write(response, HttpStatus.FORBIDDEN, "Your role is not allowed to do this.");
    }

    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(message, null));
    }
}
