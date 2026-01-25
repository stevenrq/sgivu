package com.sgivu.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

  private static final String API_DOCS_SWAGGER_CONFIG = "/v3/api-docs/swagger-config";
  private static final String REFERER_HEADER = "Referer";
  private static final String SEGMENT_REWRITE = "/${segment}";
  private static final String PURCHASE_SALE_REWRITE = "/docs/purchase-sale/(?<segment>.*)";

  @Bean
  RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    String userService = "lb://sgivu-user";
    String authService = "lb://sgivu-auth";
    String clientService = "lb://sgivu-client";
    String purchaseSaleService = "lb://sgivu-purchase-sale";
    String vehicleService = "lb://sgivu-vehicle";

    return builder
        .routes()
        // Client
        .route(
            "sgivu-v3-swagger-config-client-direct",
            r ->
                r.path("/docs/client" + API_DOCS_SWAGGER_CONFIG)
                    .filters(f -> f.rewritePath("/docs/client/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri(clientService))
        .route(
            "sgivu-v3-swagger-config-client",
            r ->
                r.path(API_DOCS_SWAGGER_CONFIG)
                    .and()
                    .header(REFERER_HEADER, ".*/docs/client/.*")
                    .uri(clientService))
        .route(
            "sgivu-client-docs",
            r ->
                r.path(
                        "/docs/client/swagger-ui.html",
                        "/docs/client/swagger-ui/**",
                        "/docs/client/v3/api-docs/**",
                        "/docs/client/webjars/**")
                    .filters(f -> f.rewritePath("/docs/client/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri(clientService))
        .route(
            "sgivu-client",
            r ->
                r.path("/v1/persons/**", "/v1/companies/**")
                    .filters(
                        f ->
                            f.tokenRelay()
                                .circuitBreaker(
                                    c ->
                                        c.setName("clientServiceCircuitBreaker")
                                            .setFallbackUri("forward:/fallback/client")))
                    .uri(clientService))
        // Auth
        .route(
            "sgivu-v3-swagger-config-auth-direct",
            r ->
                r.path("/docs/auth" + API_DOCS_SWAGGER_CONFIG)
                    .filters(f -> f.rewritePath("/docs/auth/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri(authService))
        .route(
            "sgivu-v3-swagger-config-auth",
            r ->
                r.path(API_DOCS_SWAGGER_CONFIG)
                    .and()
                    .header(REFERER_HEADER, ".*/docs/auth/.*")
                    .uri(authService))
        .route(
            "sgivu-auth-docs",
            r ->
                r.path(
                        "/docs/auth/swagger-ui.html",
                        "/docs/auth/swagger-ui/**",
                        "/docs/auth/v3/api-docs/**",
                        "/docs/auth/webjars/**")
                    .filters(f -> f.rewritePath("/docs/auth/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri(authService))
        // Gateway (documentación propia) - servir /docs/gateway/swagger-ui/** desde
        // /webjars/swagger-ui/**
        .route(
            "sgivu-gateway-swagger-webjars",
            r ->
                r.path("/docs/gateway/swagger-ui/**", "/docs/gateway/swagger-ui/index.html")
                    .filters(
                        f ->
                            f.rewritePath(
                                "/docs/gateway/swagger-ui/(?<segment>.*)",
                                "/webjars/swagger-ui/${segment}"))
                    .uri("http://127.0.0.1:8080"))
        .route(
            "sgivu-gateway-docs",
            r ->
                r.path(
                        "/docs/gateway/swagger-ui.html",
                        "/docs/gateway/swagger-ui/**",
                        "/docs/gateway/v3/api-docs/**",
                        "/docs/gateway/webjars/**")
                    .filters(f -> f.rewritePath("/docs/gateway/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri("http://127.0.0.1:8080"))
        .route(
            "sgivu-auth",
            r ->
                r.path("/v1/auth/**")
                    .filters(
                        f ->
                            f.circuitBreaker(
                                c ->
                                    c.setName("authServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/auth")))
                    .uri(authService))
        // User
        .route(
            "sgivu-v3-swagger-config-user-direct",
            r ->
                r.path("/docs/user" + API_DOCS_SWAGGER_CONFIG)
                    .filters(f -> f.rewritePath("/docs/user/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri(userService))
        .route(
            "sgivu-v3-swagger-config-user",
            r ->
                r.path(API_DOCS_SWAGGER_CONFIG)
                    .and()
                    .header(REFERER_HEADER, ".*/docs/user/.*")
                    .uri(userService))
        .route(
            "sgivu-user-docs",
            r ->
                r.path(
                        "/docs/user/swagger-ui.html",
                        "/docs/user/swagger-ui/**",
                        "/docs/user/v3/api-docs/**",
                        "/docs/user/webjars/**")
                    .filters(f -> f.rewritePath("/docs/user/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri(userService))
        .route(
            "sgivu-user",
            r ->
                r.path("/v1/users/**", "/v1/roles/**", "/v1/permissions/**")
                    .filters(
                        f ->
                            f.tokenRelay()
                                .circuitBreaker(
                                    c ->
                                        c.setName("userServiceCircuitBreaker")
                                            .setFallbackUri("forward:/fallback/user")))
                    .uri(userService))
        // Vehicle docs: Exponer OpenAPI UI/JSON bajo /docs/vehicle
        .route(
            "sgivu-v3-swagger-config-vehicle-direct",
            r ->
                r.path("/docs/vehicle" + API_DOCS_SWAGGER_CONFIG)
                    .filters(f -> f.rewritePath("/docs/vehicle/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri(vehicleService))
        .route(
            "sgivu-v3-swagger-config-vehicle",
            r ->
                r.path(API_DOCS_SWAGGER_CONFIG)
                    .and()
                    .header(REFERER_HEADER, ".*/docs/vehicle/.*")
                    .uri(vehicleService))
        .route(
            "sgivu-vehicle-docs",
            r ->
                r.path(
                        "/docs/vehicle/swagger-ui.html",
                        "/docs/vehicle/swagger-ui/**",
                        "/docs/vehicle/v3/api-docs/**",
                        "/docs/vehicle/webjars/**")
                    .filters(f -> f.rewritePath("/docs/vehicle/(?<segment>.*)", SEGMENT_REWRITE))
                    .uri("lb://sgivu-vehicle"))
        .route(
            "sgivu-vehicle",
            r ->
                r.path("/v1/vehicles/**", "/v1/cars/**", "/v1/motorcycles/**")
                    .filters(
                        f ->
                            f.tokenRelay()
                                .circuitBreaker(
                                    c ->
                                        c.setName("vehicleServiceCircuitBreaker")
                                            .setFallbackUri("forward:/fallback/vehicle")))
                    .uri(vehicleService))
        // Purchase-Sale
        .route(
            "sgivu-v3-swagger-config-purchase-sale-direct",
            r ->
                r.path("/docs/purchase-sale" + API_DOCS_SWAGGER_CONFIG)
                    .filters(f -> f.rewritePath(PURCHASE_SALE_REWRITE, SEGMENT_REWRITE))
                    .uri(purchaseSaleService))
        // Coincidencia exacta para /docs/purchase-sale/v3/api-docs (algunos clientes solicitan sin
        // barra diagonal final)
        .route(
            "sgivu-v3-api-docs-purchase-sale-exact",
            r ->
                r.path("/docs/purchase-sale/v3/api-docs")
                    .filters(f -> f.rewritePath(PURCHASE_SALE_REWRITE, SEGMENT_REWRITE))
                    .uri(purchaseSaleService))
        .route(
            "sgivu-v3-swagger-config-purchase-sale",
            r ->
                r.path(API_DOCS_SWAGGER_CONFIG)
                    .and()
                    .header(REFERER_HEADER, ".*/docs/purchase-sale/.*")
                    .uri(purchaseSaleService))
        .route(
            "sgivu-purchase-sale-docs",
            r ->
                r.path(
                        "/docs/purchase-sale/swagger-ui.html",
                        "/docs/purchase-sale/swagger-ui/**",
                        "/docs/purchase-sale/v3/api-docs/**",
                        "/docs/purchase-sale/webjars/**")
                    .filters(f -> f.rewritePath(PURCHASE_SALE_REWRITE, SEGMENT_REWRITE))
                    .uri(purchaseSaleService))
        .route(
            "sgivu-purchase-sale",
            r ->
                r.path("/v1/purchase-sales/**")
                    .filters(
                        f ->
                            f.tokenRelay()
                                .circuitBreaker(
                                    c ->
                                        c.setName("purchaseSaleServiceCircuitBreaker")
                                            .setFallbackUri("forward:/fallback/purchase-sale")))
                    .uri("lb://sgivu-purchase-sale"))
        // ML
        .route(
            "sgivu-ml",
            r ->
                r.path("/v1/ml/**")
                    .filters(
                        f ->
                            f.tokenRelay()
                                .circuitBreaker(
                                    c ->
                                        c.setName("mlServiceCircuitBreaker")
                                            .setFallbackUri("forward:/fallback/ml")))
                    .uri("http://sgivu-ml:8000"))
        .build();
  }
}
