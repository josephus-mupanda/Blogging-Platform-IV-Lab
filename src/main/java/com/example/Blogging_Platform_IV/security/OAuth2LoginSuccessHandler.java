package com.example.Blogging_Platform_IV.security;

import com.example.Blogging_Platform_IV.controller.rest.ApiResponse;
import com.example.Blogging_Platform_IV.dto.AuthToken;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.service.AuthService;
import com.example.Blogging_Platform_IV.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Called by Spring Security when Google has confirmed who the user is. Spring Security has already
 * exchanged the authorization code for an access token and called Google's user-info API, so the
 * profile (email, name) is in the OAuth2User. This class then:
 *   1. saves the user in PostgreSQL the first time (role READER),
 *   2. gives the user the same JWT as /auth/login, so the rest of the API works the same way.
 */
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
        if (email == null || !Boolean.TRUE.equals(verified)) {
            // An unverified address could belong to somebody else, so it is never used to find an account.
            write(response, HttpStatus.UNAUTHORIZED, "Google did not give a verified email address.", null);
            return;
        }
        User user = users.findOrCreateGoogleUser(email, google.getAttribute("name"));
        AuthToken token = auth.issueToken(user.getUsername(), user.getRole());
        events.oauthLogin(user.getUsername(), email);

        // The HTTP session only held the OAuth2 login state. From here on the JWT is used, so drop it.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        write(response, HttpStatus.OK, "Google login successful.", token);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message, AuthToken token)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<AuthToken> body = status == HttpStatus.OK ? ApiResponse.ok(message, token)
                : ApiResponse.error(message, null);
        mapper.writeValue(response.getOutputStream(), body);
    }
}
