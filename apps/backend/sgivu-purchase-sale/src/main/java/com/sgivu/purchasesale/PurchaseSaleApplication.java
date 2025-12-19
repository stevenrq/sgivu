package com.sgivu.purchasesale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio SGIVU de compras y ventas de vehículos usados. Expone la
 * API REST, configura la integración con otros servicios (inventario, clientes y usuarios) y
 * publica eventos de negocio relacionados con contratos y disponibilidad de inventario.
 */
@SpringBootApplication
public class PurchaseSaleApplication {

  /** Arranca la aplicación Spring Boot para el microservicio de compras y ventas. */
  public static void main(String[] args) {
    SpringApplication.run(PurchaseSaleApplication.class, args);
  }
}
