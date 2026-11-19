package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.dto.AuthToken;
import com.example.Blogging_Platform_IV.dto.UserCreate;
import com.example.Blogging_Platform_IV.entity.Role;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.exception.BlogException;
import com.example.Blogging_Platform_IV.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authentication;
    private final JwtService jwt;
    private final UserService users;

    public AuthService(AuthenticationManager authentication, JwtService jwt, UserService users) {
        this.authentication = authentication;
        this.jwt = jwt;
        this.users = users;
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
    public AuthToken login(String username, String password) {
        Authentication result = authentication.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        String authority = result.getAuthorities().iterator().next().getAuthority();
        return issueToken(result.getName(), Role.valueOf(authority.substring("ROLE_".length())));
    }

    public AuthToken issueToken(String username, Role role) {
        return new AuthToken(jwt.generate(username, role), "Bearer", jwt.lifetimeSeconds(), username, role);
    }
}
