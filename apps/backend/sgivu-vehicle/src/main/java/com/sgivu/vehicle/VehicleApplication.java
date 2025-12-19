package com.sgivu.vehicle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de vehículos usados de SGIVU.
 *
 * <p>Expone las capacidades de inventario (autos y motos), incluyendo orquestación de imágenes en
 * S3 y seguridad basada en JWT/OAuth2 para integrarse con el ecosistema de compras, ventas y
 * contratos.
 */
@SpringBootApplication
public class VehicleApplication {

  public static void main(String[] args) {
    SpringApplication.run(VehicleApplication.class, args);
  }
}
