package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.dto.AuthToken;
import com.example.Blogging_Platform_IV.dto.UserCreate;
import com.example.Blogging_Platform_IV.entity.Role;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.exception.BlogException;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    /** Public sign-up. Anyone can become a READER or an AUTHOR, but never an ADMIN. */
    public User register(UserCreate input) {
        if (input.getRole() == Role.ADMIN) {
            throw BlogException.badRequest("Registration cannot create an ADMIN account.");
        }
        return users.create(input.getUsername(), input.getEmail(), input.getDisplayName(),
                input.getPassword(), input.getRole());
    }

    /**
     * Spring Security hashes the password it received and compares it with the BCrypt hash in the
     * database. A wrong password or an unknown username throws an AuthenticationException (401).
     */
    public AuthToken login(String username, String password, String ip) {
        Authentication result;
        try {
            result = authentication.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            events.loginFailure(username, ip);
            throw e;
        }
        events.loginSuccess(result.getName(), ip);
        String authority = result.getAuthorities().iterator().next().getAuthority();
        return issueToken(result.getName(), Role.valueOf(authority.substring("ROLE_".length())));
    }

    /** Puts the token on the blacklist until it expires, so the server refuses it from now on. */
    public void logout(String token) {
        Claims claims = jwt.parse(token);
        blacklist.revoke(token, claims.getExpiration().toInstant());
        events.tokenRevoked(claims.getSubject());
    }

    public AuthToken issueToken(String username, Role role) {
        return new AuthToken(jwt.generate(username, role), "Bearer", jwt.lifetimeSeconds(), username, role);
    }
}
