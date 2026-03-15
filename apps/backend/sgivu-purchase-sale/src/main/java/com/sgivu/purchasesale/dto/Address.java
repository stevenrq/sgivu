package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Dirección asociada al cliente (calle, número, ciudad)")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
  private Long id;
  private String street;
  private String number;
  private String city;
}
