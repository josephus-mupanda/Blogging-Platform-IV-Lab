package com.example.Blogging_Platform_IV.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
     * CORS (Cross-Origin Resource Sharing): a browser page served from one origin (for example a React
     * app on http://localhost:3000) may only call this API if the API says that origin is allowed.
     * Only the origins, methods and headers listed here are accepted. A request from any other origin
     * is refused with 403 "Invalid CORS request". Postman and curl send no Origin header, so CORS
     * does not apply to them: CORS is a browser rule, it does not replace authentication.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // The token travels in the Authorization header, so no cookies are needed.
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
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
                .cors(Customizer.withDefaults())
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
