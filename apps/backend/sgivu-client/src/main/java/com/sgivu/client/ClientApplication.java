package com.sgivu.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de clientes de SGIVU.
 * Expone las APIs usadas por inventario, compras/ventas y contratos para resolver datos maestros de
 * personas y empresas vinculadas a la gestión de vehículos usados.
 */
@SpringBootApplication
public class ClientApplication {

  /** Arranca la aplicación Spring Boot del microservicio de clientes. */
  public static void main(String[] args) {
    SpringApplication.run(ClientApplication.class, args);
  }
}
