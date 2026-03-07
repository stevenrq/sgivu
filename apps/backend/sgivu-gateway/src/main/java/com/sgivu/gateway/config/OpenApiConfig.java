package com.sgivu.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Value("${openapi.server.url:}")
  private String openapiServerUrl;

  @Bean
  OpenAPI customOpenAPI() {
    List<Server> servers = new ArrayList<>();
    servers.add(new Server().url("http://localhost:8080").description("A través del API Gateway"));
    if (openapiServerUrl != null && !openapiServerUrl.isBlank()) {
      servers.add(
          new Server().url(openapiServerUrl).description("Producción (expuesto vía Nginx)"));
    }

    return new OpenAPI()
        .info(
            new Info()
                .title("SGIVU - API Gateway")
                .version("1.0.0")
                .description(
                    "API Gateway para el Sistema de Gestión de Inventario de Vehículos Usados"
                        + " (SGIVU). Centraliza y enruta las solicitudes a los microservicios"
                        + " backend, gestionando autenticación, autorización y balanceo de carga.")
                .contact(
                    new Contact()
                        .name("Steven")
                        .email("stevenrq8@gmail.com")
                        .url("https://github.com/stevenrq"))
                .license(
                    new License().name("MIT License").url("https://opensource.org/license/MIT")))
        .servers(servers)
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Token JWT obtenido desde el Authorization Server (sgivu-auth). "
                                + "Incluye claims de roles y permisos para autorización.")));
  }
}
