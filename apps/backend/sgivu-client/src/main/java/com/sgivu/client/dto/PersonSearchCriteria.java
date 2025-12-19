package com.sgivu.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Criterios combinables para filtrar personas en búsquedas utilizadas por inventario, contratos y
 * predicción de demanda.
 */
@Getter
@Builder
public class PersonSearchCriteria {
  private final String name;
  private final String email;
  private final Long nationalId;
  private final Long phoneNumber;
  private final Boolean enabled;
  private final String city;
}
