package com.sgivu.client.dto;

import com.sgivu.client.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "Payload para actualizar datos de una persona cliente")
public class PersonUpdateRequest {

  @Schema(description = "Nombre del contacto", example = "Juan", minLength = 3, maxLength = 20)
  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "first_name", nullable = false, length = 20)
  private String firstName;

  @Schema(description = "Apellido del contacto", example = "Pérez", minLength = 3, maxLength = 20)
  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "last_name", nullable = false, length = 20)
  private String lastName;

  @Schema(description = "Dirección física del contacto")
  private Address address;

  @Schema(description = "Número de teléfono (10 dígitos)", example = "3001234567")
  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true)
  private Long phoneNumber;

  @Schema(
      description = "Correo electrónico de contacto",
      example = "juan.perez@ejemplo.com",
      minLength = 16,
      maxLength = 40)
  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  private String email;
}
