package com.sgivu.client.controller;

import com.sgivu.client.dto.CompanyResponse;
import com.sgivu.client.dto.CompanySearchCriteria;
import com.sgivu.client.entity.Company;
import com.sgivu.client.mapper.ClientMapper;
import com.sgivu.client.service.CompanyService;
import java.util.*;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para gestionar empresas dentro del catálogo de clientes SGIVU. Se usa en flujos de
 * compras/ventas y contratos para identificar sociedades que adquieren o venden vehículos usados.
 *
 * <p>Se expone para consumo interno entre microservicios vía clave compartida y para clientes
 * externos autenticados con JWT emitido por el Authorization Server.
 */
@RefreshScope
@RestController
@RequestMapping("/v1/companies")
public class CompanyController {

  private final CompanyService companyService;
  private final ClientMapper clientMapper;

  public CompanyController(CompanyService companyService, ClientMapper clientMapper) {
    this.companyService = companyService;
    this.clientMapper = clientMapper;
  }

  /**
   * Crea un cliente empresa, habilitándolo para operar contratos o pedidos de inventario.
   *
   * @param company payload con datos fiscales y de contacto
   * @param bindingResult resultado de validaciones de entrada
   * @return {@link CompanyResponse} con la entidad persistida
   */
  @PostMapping
  @PreAuthorize("hasAuthority('company:create')")
  public ResponseEntity<CompanyResponse> create(
      @RequestBody Company company, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    Company savedCompany = companyService.save(company);
    CompanyResponse response = clientMapper.toCompanyResponse(savedCompany);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Recupera datos de una empresa necesaria en contratos o gestión de facturación.
   *
   * @param id identificador de la empresa
   * @return {@link CompanyResponse} si existe, 404 en caso contrario
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<CompanyResponse> getById(@PathVariable Long id) {
    return companyService
        .findById(id)
        .map(company -> ResponseEntity.ok(clientMapper.toCompanyResponse(company)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Devuelve todas las empresas registradas para alimentar catálogos internos o integraciones de
   * abastecimiento.
   *
   * @return lista de {@link CompanyResponse}
   */
  @GetMapping
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<List<CompanyResponse>> getAll() {
    List<CompanyResponse> responses =
        companyService.findAll().stream().map(clientMapper::toCompanyResponse).toList();
    return ResponseEntity.ok(responses);
  }

  /**
   * Variante paginada para consumo por lotes en sincronizaciones con inventario o predicción.
   *
   * @param page número de página
   * @return página de {@link CompanyResponse}
   */
  @GetMapping("/page/{page}")
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<Page<CompanyResponse>> getAllPaginated(@PathVariable Integer page) {
    Page<Company> companyPage = companyService.findAll(PageRequest.of(page, 10));
    Page<CompanyResponse> responsePage = companyPage.map(clientMapper::toCompanyResponse);
    return ResponseEntity.ok(responsePage);
  }

  /**
   * Actualiza datos críticos (razón social, contacto) manteniendo consistencia en microservicios
   * que referencian la empresa.
   *
   * @param id identificador
   * @param company datos nuevos
   * @param bindingResult validaciones del payload
   * @return {@link CompanyResponse} actualizado o 404 si no existe
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('company:update')")
  public ResponseEntity<CompanyResponse> update(
      @PathVariable Long id, @RequestBody Company company, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    return companyService
        .update(id, company)
        .map(updatedCompany -> ResponseEntity.ok(clientMapper.toCompanyResponse(updatedCompany)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Elimina lógicamente una empresa cuando deja de operar en SGIVU.
   *
   * @param id identificador de la empresa
   * @return 204 si se elimina, 404 si no existe
   * <p>Mantener sincronía con contratos previos depende de la lógica de dominio de servicios
   * superiores; este endpoint solo gestiona la entidad maestra.
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('company:delete')")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    Optional<Company> companyOptional = companyService.findById(id);

    if (companyOptional.isPresent()) {
      companyService.deleteById(id);
      return ResponseEntity.noContent().build();
    }

    return ResponseEntity.notFound().build();
  }

  /**
   * Cambia el estado operativo de una empresa para autorizar o bloquear su uso en nuevos contratos
   * de compra/venta.
   *
   * @param id identificador
   * @param isEnabled estado deseado
   * @return mapa con el nuevo estado o 404 si no existe
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('company:update')")
  public ResponseEntity<Map<String, Boolean>> changeStatus(
      @PathVariable Long id, @RequestBody boolean isEnabled) {
    boolean isUpdated = companyService.changeStatus(id, isEnabled);
    if (isUpdated) {
      return ResponseEntity.ok(Collections.singletonMap("status", isEnabled));
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Retorna métricas básicas usadas por el equipo de negocio para segmentar empresas activas e
   * inactivas.
   *
   * @return mapa con totales
   */
  @GetMapping("/count")
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<Map<String, Long>> getCompanyCounts() {
    long totalCompanies = companyService.findAll().size();
    long activeCompanies = companyService.countByIsEnabled(true);
    long inactiveCompanies = totalCompanies - activeCompanies;

    Map<String, Long> counts = new HashMap<>(Map.of("totalCompanies", totalCompanies));
    counts.put("activeCompanies", activeCompanies);
    counts.put("inactiveCompanies", inactiveCompanies);

    return ResponseEntity.ok(counts);
  }

  /**
   * Búsqueda flexible por identificadores fiscales o datos de contacto para validar empresas antes
   * de cerrar contratos.
   *
   * @param taxId RUC/NIT de la empresa
   * @param companyName razón social
   * @param email correo
   * @param phoneNumber teléfono
   * @param enabled estado
   * @param city ciudad del domicilio fiscal
   * @return lista de {@link CompanyResponse} que cumplen los filtros
   * @see com.sgivu.client.specification.CompanySpecifications#withFilters(CompanySearchCriteria)
   */
  @GetMapping("/search")
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<List<CompanyResponse>> searchCompanies(
      @RequestParam(required = false) String taxId,
      @RequestParam(required = false) String companyName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long phoneNumber,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String city) {

    CompanySearchCriteria criteria =
        CompanySearchCriteria.builder()
            .taxId(trimToNull(taxId))
            .companyName(trimToNull(companyName))
            .email(trimToNull(email))
            .phoneNumber(phoneNumber)
            .enabled(enabled)
            .city(trimToNull(city))
            .build();

    List<CompanyResponse> companyResponses =
        companyService.search(criteria).stream().map(clientMapper::toCompanyResponse).toList();
    return ResponseEntity.ok(companyResponses);
  }

  /**
   * Variante paginada de la búsqueda para integraciones que consumen catálogos en lotes.
   *
   * @param page página solicitada
   * @param size tamaño de página
   * @param taxId identificador fiscal
   * @param companyName razón social
   * @param email correo
   * @param phoneNumber teléfono
   * @param enabled estado
   * @param city ciudad
   * @return página de {@link CompanyResponse} filtrada
   * @see com.sgivu.client.specification.CompanySpecifications#withFilters(CompanySearchCriteria)
   */
  @GetMapping("/search/page/{page}")
  @PreAuthorize("hasAuthority('company:read')")
  public ResponseEntity<Page<CompanyResponse>> searchCompaniesPaginated(
      @PathVariable Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String taxId,
      @RequestParam(required = false) String companyName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long phoneNumber,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String city) {

    CompanySearchCriteria criteria =
        CompanySearchCriteria.builder()
            .taxId(trimToNull(taxId))
            .companyName(trimToNull(companyName))
            .email(trimToNull(email))
            .phoneNumber(phoneNumber)
            .enabled(enabled)
            .city(trimToNull(city))
            .build();

    Page<CompanyResponse> responsePage =
        companyService
            .search(criteria, PageRequest.of(page, size))
            .map(clientMapper::toCompanyResponse);
    return ResponseEntity.ok(responsePage);
  }

  /** Normaliza texto eliminando valores vacíos para evitar filtros con espacios. */
  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
