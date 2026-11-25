package com.example.Blogging_Platform_IV.handler;

import com.example.Blogging_Platform_IV.controller.rest.ApiResponse;
import com.example.Blogging_Platform_IV.dto.AuthToken;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.service.AuthService;
import com.example.Blogging_Platform_IV.service.SecurityEvents;
import com.example.Blogging_Platform_IV.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

// Called by Spring Security after Google confirmed the user. The Google profile is in the OAuth2User.
// We save the user (first time only) and return the same JWT as the normal login.
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserService users;
    private final AuthService auth;
    private final SecurityEvents events;
    private final ObjectMapper mapper;

    public OAuth2LoginSuccessHandler(UserService users, AuthService auth, SecurityEvents events,
                                     ObjectMapper mapper) {
        this.users = users;
        this.auth = auth;
        this.events = events;
        this.mapper = mapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User google = (OAuth2User) authentication.getPrincipal();
        String email = google.getAttribute("email");
        Boolean verified = google.getAttribute("email_verified");

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (email == null || !Boolean.TRUE.equals(verified)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            mapper.writeValue(response.getOutputStream(),
                    ApiResponse.error("Google did not give a verified email address.", null));
            return;
        }

        User user = users.findOrCreateGoogleUser(email, google.getAttribute("name"));
        events.oauthLogin(user.getUsername(), email);
        AuthToken token = auth.issueToken(user);
        mapper.writeValue(response.getOutputStream(), ApiResponse.ok("Google login successful.", token));
    }
}
