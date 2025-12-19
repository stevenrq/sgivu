package com.sgivu.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Criterios de filtrado para empresas, soportando búsquedas por datos fiscales y ubicación para
 * procesos de abastecimiento y contratos.
 */
@Getter
@Builder
public class CompanySearchCriteria {
  private final String companyName;
  private final String taxId;
  private final String email;
  private final Long phoneNumber;
  private final Boolean enabled;
  private final String city;
}
