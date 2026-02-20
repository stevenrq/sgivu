package com.sgivu.gateway.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de fallback para Circuit Breaker.
 *
 * <p>Cada ruta en {@code GatewayRoutesConfig} tiene un Circuit Breaker que redirige aquí cuando el
 * microservicio destino no responde dentro del timeout configurado (3s general, 30s para ML) o
 * cuando el circuito está abierto por alta tasa de errores. Estos endpoints NO indican problemas de
 * autenticación — son exclusivamente de disponibilidad de servicio.
 *
 * @see com.sgivu.gateway.config.AppConfig
 * @see com.sgivu.gateway.config.GatewayRoutesConfig
 */
@Hidden
@RestController
@RequestMapping("/fallback")
public class FallbackController {

  private static final Logger log = LoggerFactory.getLogger(FallbackController.class);
  private static final String LATER = "Inténtelo de nuevo más tarde.";

  @GetMapping("/auth")
  public ResponseEntity<String> authServiceFallback() {
    log.warn("Circuit breaker: sgivu-auth service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de autenticación no está disponible actualmente. " + LATER);
  }

  @GetMapping("/user")
  public ResponseEntity<String> userServiceFallback() {
    log.warn("Circuit breaker: sgivu-user service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de usuarios no está disponible en este momento. " + LATER);
  }

  @GetMapping("/client")
  public ResponseEntity<String> clientServiceFallback() {
    log.warn("Circuit breaker: sgivu-client service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de clientes no está disponible en este momento. " + LATER);
  }

  @GetMapping("/vehicle")
  public ResponseEntity<String> vehicleServiceFallback() {
    log.warn("Circuit breaker: sgivu-vehicle service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de vehículos no está disponible en este momento. " + LATER);
  }

  @GetMapping("/purchase-sale")
  public ResponseEntity<String> purchaseSaleServiceFallback() {
    log.warn("Circuit breaker: sgivu-purchase-sale service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de compra-venta no está disponible en este momento. " + LATER);
  }

  @RequestMapping(
      value = "/ml",
      method = {RequestMethod.GET, RequestMethod.POST})
  public ResponseEntity<String> mlServiceFallback() {
    log.warn("Circuit breaker: sgivu-ml service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de predicción no está disponible en este momento. " + LATER);
  }
}
