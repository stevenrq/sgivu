package com.sgivu.vehicle.controller;

import com.sgivu.vehicle.dto.MotorcycleResponse;
import com.sgivu.vehicle.dto.MotorcycleSearchCriteria;
import com.sgivu.vehicle.entity.Motorcycle;
import com.sgivu.vehicle.enums.VehicleStatus;
import com.sgivu.vehicle.mapper.VehicleMapper;
import com.sgivu.vehicle.service.MotorcycleService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Endpoints REST para motocicletas usadas.
 *
 * <p>Publica operaciones CRUD y búsquedas avanzadas consumidas por el front y otros servicios
 * (contratos, pricing). Protegido con permisos específicos via JWT.
 */
@RestController
@RequestMapping("/v1/motorcycles")
public class MotorcycleController {

  private final MotorcycleService motorcycleService;
  private final VehicleMapper vehicleMapper;

  public MotorcycleController(MotorcycleService motorcycleService, VehicleMapper vehicleMapper) {
    this.motorcycleService = motorcycleService;
    this.vehicleMapper = vehicleMapper;
  }

  /**
   * Registra una nueva moto en inventario.
   *
   * @param motorcycle entidad recibida
   * @param bindingResult resultado de validación
   * @return moto persistida
   */
  @PostMapping
  @PreAuthorize("hasAuthority('motorcycle:create')")
  public ResponseEntity<MotorcycleResponse> create(
      @RequestBody Motorcycle motorcycle, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    Motorcycle savedMotorcycle = motorcycleService.save(motorcycle);
    MotorcycleResponse motorcycleResponse = vehicleMapper.toMotorcycleResponse(savedMotorcycle);
    return ResponseEntity.status(HttpStatus.CREATED).body(motorcycleResponse);
  }

  /**
   * Recupera detalle de una moto.
   *
   * @param id identificador
   * @return {@link MotorcycleResponse} o 404
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<MotorcycleResponse> getById(@PathVariable Long id) {
    return motorcycleService
        .findById(id)
        .map(vehicleMapper::toMotorcycleResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** Lista todas las motos (uso interno o catálogos reducidos). */
  @GetMapping
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<List<MotorcycleResponse>> getAll() {
    return ResponseEntity.ok(
        motorcycleService.findAll().stream().map(vehicleMapper::toMotorcycleResponse).toList());
  }

  /**
   * Lista paginada de motos para el catálogo público.
   *
   * @param page índice solicitado
   */
  @GetMapping("/page/{page}")
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<Page<MotorcycleResponse>> getAllPaginated(@PathVariable Integer page) {
    return ResponseEntity.ok(
        motorcycleService
            .findAll(PageRequest.of(page, 10))
            .map(vehicleMapper::toMotorcycleResponse));
  }

  /**
   * Reemplaza la información de la moto manteniendo unicidad de campos críticos.
   *
   * @param id identificador de la moto
   * @param motorcycle datos nuevos
   * @return moto actualizada o 404
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('motorcycle:update')")
  public ResponseEntity<MotorcycleResponse> update(
      @PathVariable Long id, @RequestBody Motorcycle motorcycle, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    return motorcycleService
        .update(id, motorcycle)
        .map(
            motorcycleUpdated ->
                ResponseEntity.ok(vehicleMapper.toMotorcycleResponse(motorcycleUpdated)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Elimina una moto del inventario.
   *
   * @param id identificador
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('motorcycle:delete')")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    if (motorcycleService.findById(id).isPresent()) {
      motorcycleService.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Cambia el estado operativo de la moto (disponible, vendida, etc.).
   *
   * @param id identificador de la moto
   * @param status nuevo estado
   * @return estado aplicado
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('motorcycle:update')")
  public ResponseEntity<Map<String, String>> changeStatus(
      @PathVariable Long id, @RequestBody VehicleStatus status) {
    if (motorcycleService.changeStatus(id, status).isPresent()) {
      return ResponseEntity.ok(Collections.singletonMap("status", status.name()));
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Conteo de disponibilidad de motos.
   *
   * @return totales y disponibles
   */
  @GetMapping("/count")
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<Map<String, Long>> getMotorcycleCounts() {
    long totalMotorcycles = motorcycleService.findAll().size();
    long availableMotorcycles = motorcycleService.countByStatus(VehicleStatus.AVAILABLE);
    long unavailableMotorcycles = totalMotorcycles - availableMotorcycles;

    Map<String, Long> counts = new HashMap<>(Map.of("totalMotorcycles", totalMotorcycles));
    counts.put("availableMotorcycles", availableMotorcycles);
    counts.put("unavailableMotorcycles", unavailableMotorcycles);

    return ResponseEntity.ok(counts);
  }

  /**
   * Aplica filtros libres para encontrar motocicletas (placa, tipo, rango de precios, etc.).
   *
   * <p>Endpoint flexible para catálogos y módulos de demanda que necesitan combinar múltiples
   * atributos sin disparar consultas manuales.
   *
   * @param plate placa
   * @param brand marca
   * @param line línea
   * @param model modelo
   * @param motorcycleType tipo de moto
   * @param transmission transmisión
   * @param city ciudad de registro
   * @param status estado de negocio
   * @param minYear año mínimo
   * @param maxYear año máximo
   * @param minCapacity cilindraje mínimo
   * @param maxCapacity cilindraje máximo
   * @param minMileage kilometraje mínimo
   * @param maxMileage kilometraje máximo
   * @param minSalePrice precio mínimo
   * @param maxSalePrice precio máximo
   * @return lista con las coincidencias
   */
  @GetMapping("/search")
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<List<MotorcycleResponse>> searchMotorcycles(
      @RequestParam(required = false) String plate,
      @RequestParam(required = false) String brand,
      @RequestParam(required = false) String line,
      @RequestParam(required = false) String model,
      @RequestParam(required = false) String motorcycleType,
      @RequestParam(required = false) String transmission,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) VehicleStatus status,
      @RequestParam(required = false) Integer minYear,
      @RequestParam(required = false) Integer maxYear,
      @RequestParam(required = false) Integer minCapacity,
      @RequestParam(required = false) Integer maxCapacity,
      @RequestParam(required = false) Integer minMileage,
      @RequestParam(required = false) Integer maxMileage,
      @RequestParam(required = false) Double minSalePrice,
      @RequestParam(required = false) Double maxSalePrice) {

    MotorcycleSearchCriteria criteria =
        MotorcycleSearchCriteria.builder()
            .plate(trimToNull(plate))
            .brand(trimToNull(brand))
            .line(trimToNull(line))
            .model(trimToNull(model))
            .motorcycleType(trimToNull(motorcycleType))
            .transmission(trimToNull(transmission))
            .cityRegistered(trimToNull(city))
            .status(status)
            .minYear(minYear)
            .maxYear(maxYear)
            .minCapacity(minCapacity)
            .maxCapacity(maxCapacity)
            .minMileage(minMileage)
            .maxMileage(maxMileage)
            .minSalePrice(minSalePrice)
            .maxSalePrice(maxSalePrice)
            .build();

    List<MotorcycleResponse> motorcycleResponses =
        motorcycleService.search(criteria).stream()
            .map(vehicleMapper::toMotorcycleResponse)
            .toList();
    return ResponseEntity.ok(motorcycleResponses);
  }

  /**
   * Variante paginada del buscador de motocicletas.
   *
   * <p>Resuelve búsquedas extensas sin sobrecargar el front ni el microservicio de predicción.
   *
   * @param page índice solicitado
   * @param size tamaño de página
   * @param plate placa
   * @param brand marca
   * @param line línea
   * @param model modelo
   * @param motorcycleType tipo de moto
   * @param transmission transmisión
   * @param city ciudad de registro
   * @param status estado
   * @param minYear año mínimo
   * @param maxYear año máximo
   * @param minCapacity cilindraje mínimo
   * @param maxCapacity cilindraje máximo
   * @param minMileage kilometraje mínimo
   * @param maxMileage kilometraje máximo
   * @param minSalePrice precio mínimo
   * @param maxSalePrice precio máximo
   * @return página de DTOs
   */
  @GetMapping("/search/page/{page}")
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<Page<MotorcycleResponse>> searchMotorcyclesPaginated(
      @PathVariable Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String plate,
      @RequestParam(required = false) String brand,
      @RequestParam(required = false) String line,
      @RequestParam(required = false) String model,
      @RequestParam(required = false) String motorcycleType,
      @RequestParam(required = false) String transmission,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) VehicleStatus status,
      @RequestParam(required = false) Integer minYear,
      @RequestParam(required = false) Integer maxYear,
      @RequestParam(required = false) Integer minCapacity,
      @RequestParam(required = false) Integer maxCapacity,
      @RequestParam(required = false) Integer minMileage,
      @RequestParam(required = false) Integer maxMileage,
      @RequestParam(required = false) Double minSalePrice,
      @RequestParam(required = false) Double maxSalePrice) {

    MotorcycleSearchCriteria criteria =
        MotorcycleSearchCriteria.builder()
            .plate(trimToNull(plate))
            .brand(trimToNull(brand))
            .line(trimToNull(line))
            .model(trimToNull(model))
            .motorcycleType(trimToNull(motorcycleType))
            .transmission(trimToNull(transmission))
            .cityRegistered(trimToNull(city))
            .status(status)
            .minYear(minYear)
            .maxYear(maxYear)
            .minCapacity(minCapacity)
            .maxCapacity(maxCapacity)
            .minMileage(minMileage)
            .maxMileage(maxMileage)
            .minSalePrice(minSalePrice)
            .maxSalePrice(maxSalePrice)
            .build();

    Page<MotorcycleResponse> responsePage =
        motorcycleService
            .search(criteria, PageRequest.of(page, size))
            .map(vehicleMapper::toMotorcycleResponse);
    return ResponseEntity.ok(responsePage);
  }

  /**
   * Normaliza parámetros opcionales devolviendo null cuando quedan vacíos tras el trim.
   *
   * @param value texto recibido en la consulta
   * @return texto limpio o null
   */
  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
