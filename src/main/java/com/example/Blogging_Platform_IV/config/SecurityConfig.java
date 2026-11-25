package com.example.Blogging_Platform_IV.config;

import com.example.Blogging_Platform_IV.filter.JwtAuthenticationFilter;
import com.example.Blogging_Platform_IV.handler.JsonSecurityHandlers;
import com.example.Blogging_Platform_IV.handler.OAuth2LoginSuccessHandler;
import com.example.Blogging_Platform_IV.service.JwtService;
import com.example.Blogging_Platform_IV.service.SecurityEvents;
import com.example.Blogging_Platform_IV.service.TokenBlacklist;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Three filter chains. Spring uses the first one that matches the URL:
// 1. csrfDemo: the demo form (session + CSRF token)
// 2. googleLogin: the Google login (needs a short session)
// 3. api: everything else, stateless with JWT
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // makes @PreAuthorize work
public class SecurityConfig {

    // BCrypt: one-way hash with a random salt. Used to hash and to check the passwords.
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Used by the login: checks the username and password (AccountUserDetailsService + BCrypt).
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Chain 1: CSRF protection stays ON here. A POST needs the token that is in the form.
    @Bean
    @Order(1)
    SecurityFilterChain csrfDemo(HttpSecurity http, JsonSecurityHandlers handlers) throws Exception {
        return http
                .securityMatcher("/csrf-demo/**")
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        handlers.write(response, HttpStatus.FORBIDDEN, "Missing or invalid CSRF token.")))
                .build();
    }

    // Chain 2: Google login with Spring Security's OAuth2 Client.
    // /oauth2/authorization/google redirects to Google, and Google comes back to /login/oauth2/code/google.
    @Bean
    @Order(2)
    SecurityFilterChain googleLogin(HttpSecurity http, OAuth2LoginSuccessHandler success,
                                    JsonSecurityHandlers handlers) throws Exception {
        return http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .oauth2Login(login -> login
                        .successHandler(success)
                        .failureHandler((request, response, exception) ->
                                handlers.write(response, HttpStatus.UNAUTHORIZED, "The Google login failed.")))
                .build();
    }

    // Chain 3: the REST and GraphQL API.
    // No session, and CSRF is off: the token is in a header, not in a cookie, so there is nothing to forge.
    // Roles are checked with @PreAuthorize on the controller methods.
    @Bean
    @Order(3)
    SecurityFilterChain api(HttpSecurity http, JwtService jwt, TokenBlacklist blacklist, SecurityEvents events,
                            JsonSecurityHandlers handlers) throws Exception {
        return http
                .cors(Customizer.withDefaults())   // uses CorsConfig
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
                // created here with new, not with @Component, so it only runs inside this chain
                .addFilterBefore(new JwtAuthenticationFilter(jwt, blacklist, events),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
