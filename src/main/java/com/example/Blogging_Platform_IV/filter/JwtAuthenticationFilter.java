package com.example.Blogging_Platform_IV.filter;

import com.example.Blogging_Platform_IV.service.JwtService;
import com.example.Blogging_Platform_IV.service.SecurityEvents;
import com.example.Blogging_Platform_IV.service.TokenBlacklist;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Runs once per request. If there is a valid "Authorization: Bearer <token>" header, the user is
// logged in for this request only (no session). Otherwise nobody is logged in and Spring answers 401.
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // The reason a token was refused. JsonSecurityHandlers uses it as the 401 message.
    public static final String TOKEN_ERROR = "jwt.error";

    private final JwtService jwt;
    private final TokenBlacklist blacklist;
    private final SecurityEvents events;

    public JwtAuthenticationFilter(JwtService jwt, TokenBlacklist blacklist, SecurityEvents events) {
        this.jwt = jwt;
        this.blacklist = blacklist;
        this.events = events;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            try {
                Claims claims = jwt.parse(token);   // checks the signature and the expiry date
                if (blacklist.isRevoked(token)) {
                    reject(request, "The token was revoked by a logout. Log in again.");
                } else {
                    logIn(claims, request);
                }
            } catch (ExpiredJwtException e) {
                reject(request, "The token has expired. Log in again.");
            } catch (JwtException | IllegalArgumentException e) {
                reject(request, "The token is not valid.");
            }
        }
        chain.doFilter(request, response);
    }

    // Puts the username and the roles of the token in the security context.
    private void logIn(Claims claims, HttpServletRequest request) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<?> roles = claims.get("roles", List.class);
        if (roles != null) {
            for (Object role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
        events.tokenUsed(claims.getSubject(), request.getRequestURI());
    }

    private void reject(HttpServletRequest request, String reason) {
        request.setAttribute(TOKEN_ERROR, reason);
        events.tokenRejected(reason, request.getRemoteAddr(), request.getRequestURI());
    }
}
