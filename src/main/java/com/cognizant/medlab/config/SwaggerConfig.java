package com.cognizant.medlab.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI configuration.
 *
 * Enabled only in dev and test profiles (springdoc.swagger-ui.enabled=false in prod).
 * Access: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI medLabOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MedLab — Medical Laboratory System API")
                .description("""
                    REST API for the MedLab Medical Laboratory System.
                    
                    **Authentication:** Use POST /api/auth/login to obtain a JWT token,
                    then click 'Authorize' and enter: Bearer {token}
                    
                    **Roles:** ROLE_ADMIN | ROLE_LAB_MANAGER | ROLE_LAB_TECH |
                               ROLE_DOCTOR | ROLE_RECEPTION | ROLE_BILLING
                    """)
                .version("2.0.0")
                .contact(new Contact()
                    .name("Cognizant Team 5")
                    .email("team5@cognizant.com"))
                .license(new License().name("Internal Use Only")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT token obtained from POST /api/auth/login")));
    }
}
