package com.sgivu.purchasesale.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Proyección ligera de cliente (persona o empresa) utilizada para adjuntar datos legibles en
 * contratos y reportes sin acoplar el modelo completo del microservicio de clientes.
 */
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
