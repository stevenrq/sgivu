package com.sgivu.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del API Gateway de SGIVU. Expone un front door común para los microservicios de
 * autenticación, gestión de usuarios, clientes, vehículos usados y operaciones de compra-venta,
 * aplicando filtros transversales (seguridad, trazabilidad y resiliencia).
 */
@SpringBootApplication
public class GatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
