package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usuario interno del ecosistema SGIVU. Se consulta en el microservicio de usuarios para validar
 * responsables de contratos y mostrar datos de contacto en la capa de presentación.
 */
@Schema(
    description = "Usuario interno con datos de contacto y usuario para trazabilidad de contratos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
  private Long id;
  private Long nationalId;
  private String firstName;
  private String lastName;
  private Address address;
  private Long phoneNumber;
  private String email;
  private String username;
}
