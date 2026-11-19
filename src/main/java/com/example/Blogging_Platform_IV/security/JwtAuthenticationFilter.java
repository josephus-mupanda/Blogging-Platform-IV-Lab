package com.example.Blogging_Platform_IV.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs once for every request, before the controllers. If the request carries
 * "Authorization: Bearer <token>", the token is checked and the user is logged in for this one
 * request. If there is no token, or the token is bad, nobody is logged in and Spring Security
 * answers 401 for every endpoint that needs a login.
 *
 * Nothing is stored between requests (no session): the token is sent again each time.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    /** The reason a token was refused, read by JsonSecurityHandlers to write the 401 message. */
    static final String TOKEN_ERROR = "jwt.error";

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
                Claims claims = jwt.parse(token);
                if (blacklist.isRevoked(token)) {
                    reject(request, "The token was revoked by a logout. Log in again.");
                    chain.doFilter(request, response);
                    return;
                }
                List<?> roles = claims.get("roles", List.class);
                List<GrantedAuthority> authorities = roles == null ? List.of() : roles.stream()
                        .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                events.tokenUsed(claims.getSubject(), request.getRequestURI());
            } catch (ExpiredJwtException e) {
                reject(request, "The token has expired. Log in again.");
            } catch (JwtException | IllegalArgumentException e) {
                reject(request, "The token is not valid.");
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, String reason) {
        request.setAttribute(TOKEN_ERROR, reason);
        events.tokenRejected(reason, request.getRemoteAddr(), request.getRequestURI());
    }
}
