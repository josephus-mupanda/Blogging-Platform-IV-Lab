package com.example.Blogging_Platform_IV.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

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
}
