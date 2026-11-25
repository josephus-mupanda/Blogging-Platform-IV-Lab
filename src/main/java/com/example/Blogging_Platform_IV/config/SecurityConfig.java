package com.example.Blogging_Platform_IV.config;

import com.example.Blogging_Platform_IV.filter.JwtAuthenticationFilter;
import com.example.Blogging_Platform_IV.handler.JsonSecurityHandlers;
import com.example.Blogging_Platform_IV.handler.OAuth2LoginSuccessHandler;
import com.example.Blogging_Platform_IV.service.AccountUserDetailsService;
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

/**
 * Three filter chains. Spring Security uses the first one whose securityMatcher fits the request,
 * so each kind of client gets the rules that suit it:
 *   1. csrfDemo  the sample HTML form: session cookie + CSRF protection ON.
 *   2. googleLogin  the Google OAuth2 login: needs a short-lived session for the login "state".
 *   3. api  everything else (REST and GraphQL): stateless JWT, CSRF OFF.
 */
@Configuration
@EnableWebSecurity
// Turns on @PreAuthorize: the role check written above a controller method runs before the method.
@EnableMethodSecurity
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
     * Chain 1: the CSRF demo form. Here CSRF protection stays ON (Spring's default). The user is
     * identified by a session cookie, which a browser sends by itself with every request to this
     * site, even one triggered by another website. The CSRF token, which the other website cannot
     * read, is what proves the form really came from this site. Every POST needs a valid token.
     */
    @Bean
    @Order(1)
    SecurityFilterChain csrfDemo(HttpSecurity http, JsonSecurityHandlers handlers) throws Exception {
        return http
                .securityMatcher("/csrf-demo/**")
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        handlers.write(response, HttpStatus.FORBIDDEN,
                                "Missing or invalid CSRF token. The request was refused.")))
                .build();
    }

    /**
     * Chain 2: Google login (Spring Security OAuth2 Client).
     *   GET /oauth2/authorization/google  redirects the browser to Google.
     *   GET /login/oauth2/code/google  is where Google sends the user back with a code.
     * Spring Security then trades the code for an access token, reads the profile from Google, and
     * calls OAuth2LoginSuccessHandler, which saves the user and returns a JWT. The flow uses a
     * session for the login "state" (a random value that stops a forged callback), so this chain is
     * not stateless like the API chain. The endpoints are GET only, so CSRF is not an issue here.
     */
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

    /**
     * Chain 3: the REST and GraphQL API.
     * - Stateless: no session, every request must carry its JWT.
     * - CSRF is off. A CSRF attack works because the browser adds the session cookie to a forged
     *   request by itself. This API has no cookie to abuse: the token is in the Authorization header,
     *   which a browser never adds on its own.
     * - Public: login, registration and the API documentation. Everything else needs a valid token.
     * - Which role may call which endpoint is decided by @PreAuthorize on the controller methods.
     */
    @Bean
    @Order(3)
    SecurityFilterChain api(HttpSecurity http, JwtService jwt, TokenBlacklist blacklist, SecurityEvents events,
                            JsonSecurityHandlers handlers) throws Exception {
        return http
                // uses the CorsConfigurationSource bean of CorsConfig
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
                .addFilterBefore(new JwtAuthenticationFilter(jwt, blacklist, events),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
