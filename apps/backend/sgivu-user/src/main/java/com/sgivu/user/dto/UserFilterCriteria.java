package com.sgivu.user.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Representa los parámetros opcionales disponibles para filtrar usuarios desde la API.
 *
 * <p>Cada propiedad es opcional y se aplica únicamente cuando contiene un valor. El filtrado se
 * realiza usando lógica de intersección (AND), por lo que los resultados deben cumplir todos los
 * criterios indicados.
 *
 * @apiNote Usado en búsquedas administrativas y sincronizaciones internas sin acoplar consultas
 *     manuales.
 */
@Getter
@Builder
public class UserFilterCriteria {

  private final String name;
  private final String username;
  private final String email;
  private final String role;
  private final Boolean enabled;
}
