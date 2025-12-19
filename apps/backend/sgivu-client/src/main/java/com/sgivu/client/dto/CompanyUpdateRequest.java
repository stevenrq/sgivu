package com.sgivu.client.dto;

import com.sgivu.client.entity.Address;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Payload de actualización para empresas; utilizado por backoffice al ajustar datos fiscales o de
 * contacto que impactan contratos y facturación.
 */
@Getter
@Setter
@ToString
public class CompanyUpdateRequest {

  @NotEmpty
  @Column(name = "company_name", nullable = false, unique = true)
  private String companyName;

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
