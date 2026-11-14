package com.rms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** OpenAPI metadata and a bearer scheme for the Swagger UI ("Authorize" button). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Retirement Home RMS API")
                        .version("1.0.0")
                        .description("Scheduling and shifts management for a retirement home."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
