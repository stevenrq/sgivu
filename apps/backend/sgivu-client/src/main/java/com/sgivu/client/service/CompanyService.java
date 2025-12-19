package com.sgivu.client.service;

import com.sgivu.client.dto.CompanySearchCriteria;
import com.sgivu.client.entity.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Servicio de dominio para clientes empresa en SGIVU.
 * @see com.sgivu.client.service.impl.CompanyServiceImpl
 */
public interface CompanyService extends ClientService<Company> {

  /**
   * Busca empresa por identificador fiscal para validar operaciones B2B.
   *
   * @param taxId RUC/NIT
   * @return empresa si existe
   */
  Optional<Company> findByTaxId(String taxId);

  /**
   * Obtiene empresa por razón social exacta.
   *
   * @param companyName razón social
   * @return empresa si existe
   */
  Optional<Company> findByCompanyName(String companyName);

  /**
   * Búsqueda parcial por razón social para autocompletar en ventas corporativas.
   *
   * @param companyName fragmento de razón social
   * @return lista de coincidencias
   */
  List<Company> findByCompanyNameContainingIgnoreCase(String companyName);

  /**
   * Búsqueda flexible sin paginar combinando filtros.
   *
   * @param criteria filtros
   * @return empresas encontradas
   */
  List<Company> search(CompanySearchCriteria criteria);

  /**
   * Búsqueda paginada usando especificaciones JPA.
   *
   * @param criteria filtros
   * @param pageable configuración de paginación
   * @return página de empresas
   */
  Page<Company> search(CompanySearchCriteria criteria, Pageable pageable);
}
