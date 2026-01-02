package com.sgivu.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Define el enrutamiento del API Gateway hacia los microservicios SGIVU, aplicando circuit breakers
 * y fallbacks para mantener la continuidad de los flujos de autenticación, inventario de vehículos
 * usados y compra-venta. Las rutas usan discovery (`lb://`) para balancear entre instancias y
 * delegan la recuperación de errores a los endpoints de fallback.
 */
@Configuration
public class GatewayRoutesConfig {

  @Bean
  RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder
        .routes()
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
                    .uri("lb://sgivu-auth"))
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
                    .uri("lb://sgivu-user"))
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
                    .uri("lb://sgivu-client"))
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
                    .uri("lb://sgivu-vehicle"))
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
