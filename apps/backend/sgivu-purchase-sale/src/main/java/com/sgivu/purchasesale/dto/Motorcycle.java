package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(description = "DTO de motocicleta con atributos adicionales para valoración")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Motorcycle extends Vehicle {
  private String motorcycleType;
}
