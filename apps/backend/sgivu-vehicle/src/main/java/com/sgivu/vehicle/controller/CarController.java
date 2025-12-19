package com.sgivu.vehicle.controller;

import com.sgivu.vehicle.dto.CarResponse;
import com.sgivu.vehicle.dto.CarSearchCriteria;
import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.enums.VehicleStatus;
import com.sgivu.vehicle.mapper.VehicleMapper;
import com.sgivu.vehicle.service.CarService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Endpoints REST para gestionar autos usados.
 *
 * <p>sExpone operaciones CRUD, conteos y búsquedas avanzadas consumidas por front y otros
 * microservicios SGIVU (ventas, contratos). Las autorizaciones se delegan a Spring Security con JWT
 * emitidos por el Authorization Server.
 */
@RestController
@RequestMapping("/v1/cars")
public class CarController {

  private final CarService carService;
  private final VehicleMapper vehicleMapper;

  public CarController(CarService carService, VehicleMapper vehicleMapper) {
    this.carService = carService;
    this.vehicleMapper = vehicleMapper;
  }

  /**
   * Registra un nuevo auto en inventario.
   *
   * @param car entidad recibida desde el frontend/servicio de compras
   * @param bindingResult validaciones de entrada
   * @return auto persistido como {@link CarResponse}
   */
  @PostMapping
  @PreAuthorize("hasAuthority('car:create')")
  public ResponseEntity<CarResponse> create(@RequestBody Car car, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    CarResponse carResponse = vehicleMapper.toCarResponse(carService.save(car));
    return ResponseEntity.status(HttpStatus.CREATED).body(carResponse);
  }

  /**
   * Obtiene detalle de un auto.
   *
   * @param id identificador interno
   * @return {@link CarResponse} o 404
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<CarResponse> getById(@PathVariable Long id) {
    return carService
        .findById(id)
        .map(car -> ResponseEntity.ok(vehicleMapper.toCarResponse(car)))
        .orElse(ResponseEntity.notFound().build());
  }

  /** Lista completa de autos (uso interno o sincronizaciones pequeñas). */
  @GetMapping
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<List<CarResponse>> getAll() {
    List<CarResponse> carResponses =
        carService.findAll().stream().map(vehicleMapper::toCarResponse).toList();
    return ResponseEntity.ok(carResponses);
  }

  /**
   * Devuelve autos paginados para catálogos públicos.
   *
   * @param page índice solicitado
   */
  @GetMapping("/page/{page}")
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<Page<CarResponse>> getAllPaginated(@PathVariable Integer page) {
    return ResponseEntity.ok(
        carService.findAll(PageRequest.of(page, 10)).map(vehicleMapper::toCarResponse));
  }

  /**
   * Reemplaza datos del auto preservando integridad de campos únicos.
   *
   * @param id identificador del auto a actualizar
   * @param car payload recibido
   * @return {@link CarResponse} actualizado o 404
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('car:update')")
  public ResponseEntity<CarResponse> update(
      @PathVariable Long id, @RequestBody Car car, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    return carService
        .update(id, car)
        .map(updatedCar -> ResponseEntity.ok(vehicleMapper.toCarResponse(updatedCar)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Elimina un auto del inventario.
   *
   * @param id identificador del auto
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('car:delete')")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    Optional<Car> carOptional = carService.findById(id);

    if (carOptional.isPresent()) {
      carService.deleteById(id);
      return ResponseEntity.noContent().build();
    }

    return ResponseEntity.notFound().build();
  }

  /**
   * Cambia el estado operativo del auto (ej. disponible → vendido).
   *
   * @param id identificador del auto
   * @param status nuevo estado de negocio
   * @return estado aplicado
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('car:update')")
  public ResponseEntity<Map<String, String>> changeStatus(
      @PathVariable Long id, @RequestBody VehicleStatus status) {
    if (carService.changeStatus(id, status).isPresent()) {
      return ResponseEntity.ok(Collections.singletonMap("status", status.name()));
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Resumen de disponibilidad de autos.
   *
   * @return totales y disponibles, usado para tableros de inventario
   */
  @GetMapping("/count")
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<Map<String, Long>> getCarCounts() {
    long totalCars = carService.findAll().size();
    long availableCars = carService.countByStatus(VehicleStatus.AVAILABLE);
    long unavailableCars = totalCars - availableCars;

    Map<String, Long> counts = new HashMap<>(Map.of("totalCars", totalCars));
    counts.put("availableCars", availableCars);
    counts.put("unavailableCars", unavailableCars);

    return ResponseEntity.ok(counts);
  }

  /**
   * Busca autos aplicando múltiples filtros opcionales (placa, marca, rangos numéricos, etc.).
   *
   * <p>Combinación flexible pensada para catálogos y predicción de demanda sin necesidad de
   * endpoints adicionales.
   *
   * @param plate placa parcial o completa
   * @param brand marca
   * @param line línea
   * @param model modelo
   * @param fuelType combustible
   * @param bodyType carrocería
   * @param transmission transmisión
   * @param city ciudad de registro
   * @param status estado de negocio
   * @param minYear año mínimo
   * @param maxYear año máximo
   * @param minCapacity capacidad mínima
   * @param maxCapacity capacidad máxima
   * @param minMileage kilometraje mínimo
   * @param maxMileage kilometraje máximo
   * @param minSalePrice precio mínimo
   * @param maxSalePrice precio máximo
   * @return lista de autos que cumplen los filtros
   */
  @GetMapping("/search")
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<List<CarResponse>> searchCars(
      @RequestParam(required = false) String plate,
      @RequestParam(required = false) String brand,
      @RequestParam(required = false) String line,
      @RequestParam(required = false) String model,
      @RequestParam(required = false) String fuelType,
      @RequestParam(required = false) String bodyType,
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

    CarSearchCriteria criteria =
        CarSearchCriteria.builder()
            .plate(trimToNull(plate))
            .brand(trimToNull(brand))
            .line(trimToNull(line))
            .model(trimToNull(model))
            .fuelType(trimToNull(fuelType))
            .bodyType(trimToNull(bodyType))
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

    List<CarResponse> carResponses =
        carService.search(criteria).stream().map(vehicleMapper::toCarResponse).toList();
    return ResponseEntity.ok(carResponses);
  }

  /**
   * Variante paginada de {@link #searchCars}.
   *
   * @param page índice de página
   * @param size tamaño de página
   * @return página de resultados
   */
  @GetMapping("/search/page/{page}")
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<Page<CarResponse>> searchCarsPaginated(
      @PathVariable Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String plate,
      @RequestParam(required = false) String brand,
      @RequestParam(required = false) String line,
      @RequestParam(required = false) String model,
      @RequestParam(required = false) String fuelType,
      @RequestParam(required = false) String bodyType,
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

    CarSearchCriteria criteria =
        CarSearchCriteria.builder()
            .plate(trimToNull(plate))
            .brand(trimToNull(brand))
            .line(trimToNull(line))
            .model(trimToNull(model))
            .fuelType(trimToNull(fuelType))
            .bodyType(trimToNull(bodyType))
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

    Page<CarResponse> pageResponse =
        carService.search(criteria, PageRequest.of(page, size)).map(vehicleMapper::toCarResponse);
    return ResponseEntity.ok(pageResponse);
  }

  /**
   * Normaliza texto opcional eliminando espacios y devolviendo null si queda vacío.
   *
   * @param value valor recibido en query params
   * @return texto limpio o null
   */
  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
