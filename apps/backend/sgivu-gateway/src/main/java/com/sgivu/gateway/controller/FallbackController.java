package com.sgivu.gateway.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Controlador de fallback para Circuit Breaker.
 *
 * <p>Cada ruta en {@code GatewayRoutesConfig} tiene un Circuit Breaker que redirige aquí cuando el
 * microservicio destino no responde dentro del timeout configurado (10s general, 30s para ML, 1800s
 * para retrain) o cuando el circuito está abierto por alta tasa de errores. Estos endpoints NO
 * indican problemas de autenticación — son exclusivamente de disponibilidad de servicio.
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

  @RequestMapping(
      value = "/auth",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<String> authServiceFallback() {
    log.warn("Circuit breaker: sgivu-auth service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de autenticación no está disponible actualmente. " + LATER);
  }

  @RequestMapping(
      value = "/user",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<String> userServiceFallback() {
    log.warn("Circuit breaker: sgivu-user service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de usuarios no está disponible en este momento. " + LATER);
  }

  @RequestMapping(
      value = "/client",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<String> clientServiceFallback() {
    log.warn("Circuit breaker: sgivu-client service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de clientes no está disponible en este momento. " + LATER);
  }

  @RequestMapping(
      value = "/vehicle",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<String> vehicleServiceFallback() {
    log.warn("Circuit breaker: sgivu-vehicle service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de vehículos no está disponible en este momento. " + LATER);
  }

  @RequestMapping(
      value = "/purchase-sale",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<String> purchaseSaleServiceFallback() {
    log.warn("Circuit breaker: sgivu-purchase-sale service unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("El servicio de compra-venta no está disponible en este momento. " + LATER);
  }

  @RequestMapping(
      value = "/ml",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<Map<String, String>> mlServiceFallback(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().value();
    log.warn("Circuit breaker: sgivu-ml service unavailable [path={}]", path);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Map.of(
                "detail",
                "El servicio de predicción no está disponible en este momento. " + LATER));
  }

  @RequestMapping(
      value = "/ml-retrain",
      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
  public ResponseEntity<Map<String, String>> mlRetrainFallback(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().value();
    log.error(
        "mlRetrainCircuitBreaker triggered [path={}] — el reentrenamiento superó el tiempo "
            + "máximo de 30 minutos o el servicio ML no está disponible. "
            + "Verifica que sgivu-ml esté corriendo y que el dataset no sea excesivamente grande.",
        path);
    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Map.of(
                "detail",
                "El reentrenamiento superó el tiempo máximo de 30 minutos. "
                    + "El modelo anterior sigue activo. "
                    + LATER));
  }
}
