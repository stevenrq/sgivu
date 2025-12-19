package com.sgivu.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor Eureka del ecosistema SGIVU que centraliza el descubrimiento de los microservicios de
 * inventario, compras/ventas, contratos y predicción de demanda. Todas las integraciones HTTP entre
 * servicios (gateway, clientes declarativos y HTTP Interface) dependen de este registro para
 * resolver endpoints dinámicos y aplicar políticas de resiliencia.
 *
 * @apiNote Debe iniciarse antes que `sgivu-gateway` y el resto de microservicios para evitar
 *     timeouts iniciales y circuit breakers abiertos durante el arranque.
 */
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryApplication {

  public static void main(String[] args) {
    SpringApplication.run(DiscoveryApplication.class, args);
  }
}
