package com.docqueue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration.
 * Accessible at:
 *  - Swagger UI:  /swagger-ui.html
 *  - JSON spec:   /api-docs
 */
@Configuration
public class SwaggerConfig {

    @Value("${FRONTEND_URL:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Doctor Appointment & Queue Management API")
                        .description("""
                            Production-grade REST API for the Doctor Appointment & Real-Time
                            Queue Management SaaS Platform.
                            
                            **Roles:** PATIENT | DOCTOR | ADMIN
                            
                            **Auth:** Use the `/api/v1/auth/login` endpoint to get a JWT token,
                            then click 'Authorize' and enter `Bearer <your-token>`.
                            """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("DocQueue Support")
                                .email("support@docqueue.in"))
                        .license(new License()
                                .name("Private — All Rights Reserved")))
                .servers(List.of(
                        new Server().url(serverUrl).description("Current Server"),
                        new Server().url("http://localhost:8080").description("Local Development")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .name("Bearer Authentication")
                                        .description("Enter JWT token obtained from /api/v1/auth/login")));
    }
}
