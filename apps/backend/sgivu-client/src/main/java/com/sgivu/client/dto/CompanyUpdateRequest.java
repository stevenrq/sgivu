package com.sgivu.client.dto;

import com.sgivu.client.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "Payload para actualizar datos de una empresa cliente")
public class CompanyUpdateRequest {

  @Schema(description = "Razón social de la empresa", example = "ACME SA")
  @NotEmpty
  @Column(name = "company_name", nullable = false, unique = true)
  private String companyName;

  @Schema(description = "Dirección fiscal de la empresa")
  private Address address;

  @Schema(description = "Número de teléfono de contacto", example = "6013001234")
  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true)
  private Long phoneNumber;

  @Schema(
      description = "Correo electrónico de la empresa",
      example = "contacto@acme.com",
      minLength = 16,
      maxLength = 40)
  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  private String email;
}
