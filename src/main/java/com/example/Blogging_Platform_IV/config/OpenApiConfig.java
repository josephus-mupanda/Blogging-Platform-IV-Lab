
package com.example.Blogging_Platform_IV.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI bloggingApi() {
        return new OpenAPI().info(new Info().title("Blogging Platform IV").version("1.0"));
    }
}
