package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(description = "Datos de un cliente persona natural (identificación y nombres)")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Person extends Client {
  private Long nationalId;
  private String firstName;
  private String lastName;
}
