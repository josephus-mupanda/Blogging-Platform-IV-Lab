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

// Writes the security errors as JSON, like the rest of the API:
// 401 when nobody is logged in, 403 when the role is not allowed.
@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;
    private final SecurityEvents events;

    public JsonSecurityHandlers(ObjectMapper mapper, SecurityEvents events) {
        this.mapper = mapper;
        this.events = events;
    }

    // 401
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR);
        String message = "Log in first: send a Bearer token.";
        if (reason != null) {
            message = reason.toString();   // the token was bad: the filter already logged it
        } else {
            events.unauthenticatedRequest(request.getRemoteAddr(), request.getRequestURI());
        }
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, message);
    }

    // 403
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "unknown";
        events.accessDenied(username, request.getRequestURI());
        write(response, HttpStatus.FORBIDDEN, "Your role is not allowed to do this.");
    }

    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(message, null));
    }
}
