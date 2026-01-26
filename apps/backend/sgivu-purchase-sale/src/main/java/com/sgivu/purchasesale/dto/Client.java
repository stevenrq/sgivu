package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(description = "Modelo base de cliente con datos de contacto y estado de habilitación")
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class Client {
  private Long id;
  private Address address;
  private Long phoneNumber;
  private String email;
  private boolean enabled;
}
