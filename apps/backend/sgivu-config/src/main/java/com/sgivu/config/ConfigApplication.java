package com.sgivu.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Aplicacion Spring Cloud Config que centraliza la configuracion compartida por los microservicios
 * SGIVU, garantizando consistencia en endpoints de integracion, contratos, politicas de seguridad
 * (JWT/OAuth2) y parametros de resiliencia usados por cada servicio de inventario, compras, ventas
 * y prediccion.
 *
 * @apiNote Actua como punto unico para versionar propiedades en Git y soportar actualizaciones
 *     dinamicas sin redeploy de los microservicios consumidores.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigApplication.class, args);
  }
}
