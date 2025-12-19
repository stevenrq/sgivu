package com.sgivu.purchasesale.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usuario interno del ecosistema SGIVU. Se consulta en el microservicio de usuarios para validar
 * responsables de contratos y mostrar datos de contacto en la capa de presentación.
 */
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
