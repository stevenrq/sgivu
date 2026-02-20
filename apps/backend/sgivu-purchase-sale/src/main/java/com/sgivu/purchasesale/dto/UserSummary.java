package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Schema(
    description =
        "Datos mínimos del usuario interno (id, nombre completo y contacto) utilizados en reportes"
            + " y contratos")
@Value
@Builder
public class UserSummary {
  Long id;
  String fullName;
  String email;
  String username;
}
