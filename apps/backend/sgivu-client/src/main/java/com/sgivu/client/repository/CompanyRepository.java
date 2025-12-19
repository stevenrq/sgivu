package com.sgivu.client.repository;

import com.sgivu.client.entity.Company;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de empresas con consultas por identificador fiscal y razón social para soportar
 * procesos B2B.
 */
public interface CompanyRepository extends ClientRepository<Company> {
  /**
   * Busca empresa por identificador fiscal.
   *
   * @param taxId RUC/NIT
   * @return empresa encontrada
   */
  Optional<Company> findByTaxId(String taxId);

  /**
   * Recupera empresa por razón social exacta.
   *
   * @param companyName razón social
   * @return empresa si existe
   */
  Optional<Company> findByCompanyName(String companyName);

  /**
   * Búsqueda parcial por razón social para autocompletar.
   *
   * @param companyName fragmento
   * @return coincidencias
   */
  List<Company> findByCompanyNameContainingIgnoreCase(String companyName);
}
