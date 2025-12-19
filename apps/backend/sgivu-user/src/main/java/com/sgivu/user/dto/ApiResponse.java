package com.sgivu.user.dto;

import java.util.Map;
import lombok.*;

/**
 * Envoltura estándar para respuestas REST del microservicio de usuarios.
 *
 * <p>Permite retornar datos o errores de validación de forma homogénea hacia el API Gateway y otros
 * consumidores.
 *
 * @param <T> El tipo de dato de la respuesta.
 */
@NoArgsConstructor
@Getter
@Setter
public class ApiResponse<T> {

  private T data;

  private Map<String, String> errors;

  /**
   * Construye una respuesta exitosa con datos y sin errores.
   *
   * @param data carga útil a devolver al cliente.
   */
  public ApiResponse(T data) {
    this.data = data;
    this.errors = null;
  }

  /**
   * Construye una respuesta de error de validación o negocio.
   *
   * @param errors mapa de campo a mensaje descriptivo.
   */
  public ApiResponse(Map<String, String> errors) {
    this.errors = errors;
    this.data = null;
  }
}
