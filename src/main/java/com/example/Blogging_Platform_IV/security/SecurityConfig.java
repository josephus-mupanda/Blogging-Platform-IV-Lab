package com.example.Blogging_Platform_IV.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt hashes a password with a random salt and a work factor, so the same password gives a
     * different hash each time and the hash is slow to guess. Passwords are never stored in clear.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Checks a username and password against the database (AccountUserDetailsService + BCrypt). */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * The chain for the REST and GraphQL API.
     * - Stateless: no session, every request must carry its JWT.
     * - CSRF is off. A CSRF attack works because the browser adds the session cookie to a forged
     *   request by itself. This API has no cookie to abuse: the token is in the Authorization header,
     *   which a browser never adds on its own.
     * - Public: login, registration and the API documentation. Everything else needs a valid token.
     * - Which role may call which endpoint is decided by @PreAuthorize on the controller methods.
     */
    @Bean
    SecurityFilterChain api(HttpSecurity http, JwtService jwt, JsonSecurityHandlers handlers) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/graphiql/**",
                                "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                // Not a @Component: Spring Boot would also register a filter bean on its own,
                // outside the security chain.
                .addFilterBefore(new JwtAuthenticationFilter(jwt), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
