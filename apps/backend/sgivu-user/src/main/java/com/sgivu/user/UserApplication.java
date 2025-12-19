package com.sgivu.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del microservicio de usuarios de SGIVU. */
@SpringBootApplication
public class UserApplication {

  /**
   * Arranca el contexto Spring Boot del servicio de usuarios.
   *
   * @param args argumentos de línea de comandos delegados a Spring.
   */
  public static void main(String[] args) {
    SpringApplication.run(UserApplication.class, args);
  }
}
