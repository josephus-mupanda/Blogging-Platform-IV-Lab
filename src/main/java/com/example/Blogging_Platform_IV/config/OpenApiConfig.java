
package com.example.Blogging_Platform_IV.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String BEARER = "bearerAuth";

    // Swagger shows every endpoint as secured by a JWT (padlock + Authorize button).
    // The public endpoints in AuthController remove this with @SecurityRequirements.
    @Bean
    OpenAPI bloggingApi() {
        return new OpenAPI()
                .info(new Info().title("Blogging Platform IV").version("1.0"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
