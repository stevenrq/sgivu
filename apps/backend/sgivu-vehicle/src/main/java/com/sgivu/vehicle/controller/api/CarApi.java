package com.sgivu.vehicle.controller.api;

import com.sgivu.vehicle.dto.CarResponse;
import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autos", description = "Operaciones CRUD y búsqueda de autos del sistema")
@RequestMapping("/v1/cars")
public interface CarApi {

  @Operation(summary = "Alta de auto", description = "Registra un nuevo auto en inventario")
  @ApiResponse(
      responseCode = "201",
      description = "Creado",
      content = @Content(schema = @Schema(implementation = CarResponse.class)))
  @ApiResponse(responseCode = "400", description = "Datos inválidos")
  @PostMapping
  ResponseEntity<CarResponse> create(
      @Valid @RequestBody @Parameter(description = "Entidad Car") Car car,
      BindingResult bindingResult);

  @Operation(summary = "Obtiene detalle de un auto")
  @GetMapping("/{id}")
  ResponseEntity<CarResponse> getById(
      @PathVariable @Parameter(description = "ID del auto") Long id);

  @Hidden
  @GetMapping
  ResponseEntity<List<CarResponse>> getAll();

  @Operation(summary = "Listado paginado de autos")
  @GetMapping("/page/{page}")
  ResponseEntity<Page<CarResponse>> getAllPaginated(
      @PathVariable @Parameter(description = "Índice de página") Integer page);

  @Operation(summary = "Actualiza un auto por id")
  @PutMapping("/{id}")
  ResponseEntity<CarResponse> update(
      @PathVariable @Parameter(description = "ID del auto") Long id,
      @Valid @RequestBody @Parameter(description = "Entidad Car") Car car,
      BindingResult bindingResult);

  @Operation(summary = "Elimina un auto por id")
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteById(@PathVariable @Parameter(description = "ID del auto") Long id);

  @Operation(summary = "Cambia el estado del auto")
  @PatchMapping("/{id}/status")
  ResponseEntity<java.util.Map<String, String>> changeStatus(
      @PathVariable @Parameter(description = "ID del auto") Long id,
      @RequestBody @Parameter(description = "Nuevo estado del vehículo") VehicleStatus status);

  @Operation(summary = "Conteo de autos")
  @GetMapping("/count")
  ResponseEntity<java.util.Map<String, Long>> getCarCounts();

  @Operation(summary = "Busca autos con múltiples filtros")
  @GetMapping("/search")
  ResponseEntity<List<CarResponse>> searchCars(
      @RequestParam(required = false) @Parameter(description = "Placa parcial o completa")
          String plate,
      @RequestParam(required = false) @Parameter(description = "Marca") String brand,
      @RequestParam(required = false) @Parameter(description = "Línea") String line,
      @RequestParam(required = false) @Parameter(description = "Modelo") String model,
      @RequestParam(required = false) @Parameter(description = "Combustible") String fuelType,
      @RequestParam(required = false) @Parameter(description = "Carrocería") String bodyType,
      @RequestParam(required = false) @Parameter(description = "Transmisión") String transmission,
      @RequestParam(required = false) @Parameter(description = "Ciudad registrada") String city,
      @RequestParam(required = false) @Parameter(description = "Estado del vehículo")
          VehicleStatus status,
      @RequestParam(required = false) @Parameter(description = "Año mínimo") Integer minYear,
      @RequestParam(required = false) @Parameter(description = "Año máximo") Integer maxYear,
      @RequestParam(required = false) @Parameter(description = "Capacidad mínima")
          Integer minCapacity,
      @RequestParam(required = false) @Parameter(description = "Capacidad máxima")
          Integer maxCapacity,
      @RequestParam(required = false) @Parameter(description = "Kilometraje mínimo")
          Integer minMileage,
      @RequestParam(required = false) @Parameter(description = "Kilometraje máximo")
          Integer maxMileage,
      @RequestParam(required = false) @Parameter(description = "Precio mínimo") Double minSalePrice,
      @RequestParam(required = false) @Parameter(description = "Precio máximo")
          Double maxSalePrice);

  @Operation(summary = "Búsqueda de autos paginada")
  @GetMapping("/search/page/{page}")
  ResponseEntity<Page<CarResponse>> searchCarsPaginated(
      @PathVariable @Parameter(description = "Página") Integer page,
      @RequestParam(defaultValue = "10") @Parameter(description = "Tamaño de página") Integer size,
      @RequestParam(required = false) @Parameter(description = "Placa") String plate,
      @RequestParam(required = false) @Parameter(description = "Marca") String brand,
      @RequestParam(required = false) @Parameter(description = "Línea") String line,
      @RequestParam(required = false) @Parameter(description = "Modelo") String model,
      @RequestParam(required = false) @Parameter(description = "Combustible") String fuelType,
      @RequestParam(required = false) @Parameter(description = "Carrocería") String bodyType,
      @RequestParam(required = false) @Parameter(description = "Transmisión") String transmission,
      @RequestParam(required = false) @Parameter(description = "Ciudad registrada") String city,
      @RequestParam(required = false) @Parameter(description = "Estado") VehicleStatus status,
      @RequestParam(required = false) @Parameter(description = "Año mínimo") Integer minYear,
      @RequestParam(required = false) @Parameter(description = "Año máximo") Integer maxYear,
      @RequestParam(required = false) @Parameter(description = "Capacidad mínima")
          Integer minCapacity,
      @RequestParam(required = false) @Parameter(description = "Capacidad máxima")
          Integer maxCapacity,
      @RequestParam(required = false) @Parameter(description = "Kilometraje mínimo")
          Integer minMileage,
      @RequestParam(required = false) @Parameter(description = "Kilometraje máximo")
          Integer maxMileage,
      @RequestParam(required = false) @Parameter(description = "Precio mínimo") Double minSalePrice,
      @RequestParam(required = false) @Parameter(description = "Precio máximo")
          Double maxSalePrice);
}
