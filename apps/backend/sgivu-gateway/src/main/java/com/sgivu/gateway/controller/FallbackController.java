package com.sgivu.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Respuestas controladas de degradación cuando un microservicio cae o el circuit breaker se abre.
 * Mantiene mensajes consistentes hacia el front mientras se preserva la estabilidad de los flujos
 * de autenticación, usuarios, clientes, inventario y compra-venta. Es invocado por las rutas
 * configuradas en {@link com.sgivu.gateway.config.GatewayRoutesConfig} a través de {@code
 * forward:/fallback/**}.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

  private static final String LATER = "Inténtelo de nuevo más tarde.";

  @GetMapping("/auth")
  public ResponseEntity<String> authServiceFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de autenticación no está disponible actualmente. " + LATER);
  }

  @GetMapping("/user")
  public ResponseEntity<String> userServiceFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de usuarios no está disponible en este momento. " + LATER);
  }

  @GetMapping("/client")
  public ResponseEntity<String> clientServiceFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de clientes no está disponible en este momento. " + LATER);
  }

  @GetMapping("/vehicle")
  public ResponseEntity<String> vehicleServiceFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de vehículos no está disponible en este momento. " + LATER);
  }

  @GetMapping("/purchase-sale")
  public ResponseEntity<String> purchaseSaleServiceFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de compra-venta no está disponible en este momento. " + LATER);
  }

  @RequestMapping(
      value = "/ml",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> mlServiceFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de predicción no está disponible en este momento. " + LATER);
  }
}
