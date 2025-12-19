package com.sgivu.client.dto;

import com.sgivu.client.entity.Address;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Payload para actualizar datos de una persona cuando cambia su información de contacto en
 * procesos de postventa o renovación de contratos.
 */
@Getter
@Setter
@ToString
public class PersonUpdateRequest {

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "first_name", nullable = false, length = 20)
  private String firstName;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "last_name", nullable = false, length = 20)
  private String lastName;

  private Address address;

  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true)
  private Long phoneNumber;

  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  private String email;
}
