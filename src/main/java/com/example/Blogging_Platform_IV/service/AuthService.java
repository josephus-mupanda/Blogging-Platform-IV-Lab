package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.dto.AuthToken;
import com.example.Blogging_Platform_IV.dto.UserCreate;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.enums.Role;
import com.example.Blogging_Platform_IV.exception.BlogException;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authentication;
    private final JwtService jwt;
    private final UserService users;
    private final TokenBlacklist blacklist;
    private final SecurityEvents events;

    public AuthService(AuthenticationManager authentication, JwtService jwt, UserService users,
                       TokenBlacklist blacklist, SecurityEvents events) {
        this.authentication = authentication;
        this.jwt = jwt;
        this.users = users;
        this.blacklist = blacklist;
        this.events = events;
    }

    // Anyone can register as READER or AUTHOR, but not as ADMIN.
    public User register(UserCreate input) {
        if (input.getRole() == Role.ADMIN) {
            throw BlogException.badRequest("Registration cannot create an ADMIN account.");
        }
        return users.create(input.getUsername(), input.getEmail(), input.getDisplayName(),
                input.getPassword(), input.getRole());
    }

    // Spring Security checks the password against the BCrypt hash. A wrong login throws an
    // AuthenticationException, which becomes a 401.
    public AuthToken login(String username, String password, String ip) {
        try {
            authentication.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            events.loginFailure(username, ip);
            throw e;
        }
        events.loginSuccess(username, ip);
        return issueToken(users.getByUsername(username));
    }

    // The token goes on the blacklist until it expires.
    public void logout(String token) {
        Claims claims = jwt.parse(token);
        blacklist.revoke(token, claims.getExpiration().toInstant());
        events.tokenRevoked(claims.getSubject());
    }

    public AuthToken issueToken(User user) {
        String token = jwt.generate(user.getUsername(), user.getRole());
        return new AuthToken(token, "Bearer", jwt.lifetimeSeconds(), user.getUsername(), user.getRole());
    }
}
