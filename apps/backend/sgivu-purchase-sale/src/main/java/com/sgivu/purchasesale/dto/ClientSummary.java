package com.sgivu.purchasesale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Schema(
    description =
        "Proyección ligera de cliente (persona o empresa) con datos relevantes para contratos y"
            + " reportes")
@Value
@Builder
public class ClientSummary {
  Long id;
  String type;
  String name;
  String identifier;
  String email;
  Long phoneNumber;
}
