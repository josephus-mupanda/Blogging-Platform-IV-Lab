
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

    /**
     * Every endpoint is documented as secured by a JWT (the padlock and the Authorize button in
     * Swagger UI). The two public endpoints in AuthController remove that requirement with
     * @SecurityRequirements.
     */
    @Bean
    OpenAPI bloggingApi() {
        return new OpenAPI()
                .info(new Info().title("Blogging Platform IV").version("1.0"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
