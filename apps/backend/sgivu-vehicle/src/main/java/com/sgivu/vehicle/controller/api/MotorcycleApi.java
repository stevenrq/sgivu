package com.sgivu.vehicle.controller.api;

import com.sgivu.vehicle.dto.MotorcycleResponse;
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

@Tag(name = "Motocicletas", description = "Operaciones CRUD y búsqueda de motocicletas")
@RequestMapping("/v1/motorcycles")
public interface MotorcycleApi {

  @Operation(summary = "Alta de motocicleta")
  @ApiResponse(
      responseCode = "201",
      description = "Creado",
      content = @Content(schema = @Schema(implementation = MotorcycleResponse.class)))
  @ApiResponse(responseCode = "400", description = "Datos inválidos")
  @PostMapping
  ResponseEntity<MotorcycleResponse> create(
      @Valid @RequestBody @Parameter(description = "Entidad Motorcycle")
          com.sgivu.vehicle.entity.Motorcycle motorcycle,
      BindingResult bindingResult);

  @Operation(summary = "Obtiene detalle de una moto")
  @GetMapping("/{id}")
  ResponseEntity<MotorcycleResponse> getById(@PathVariable @Parameter(description = "ID") Long id);

  @Hidden
  @GetMapping
  ResponseEntity<List<MotorcycleResponse>> getAll();

  @Operation(summary = "Listado paginado")
  @GetMapping("/page/{page}")
  ResponseEntity<Page<MotorcycleResponse>> getAllPaginated(
      @PathVariable @Parameter(description = "Pág") Integer page);

  @Operation(summary = "Actualiza una motocicleta")
  @PutMapping("/{id}")
  ResponseEntity<MotorcycleResponse> update(
      @PathVariable @Parameter(description = "ID") Long id,
      @Valid @RequestBody @Parameter(description = "Entidad Motorcycle")
          com.sgivu.vehicle.entity.Motorcycle motorcycle,
      BindingResult bindingResult);

  @Operation(summary = "Elimina una motocicleta")
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteById(@PathVariable @Parameter(description = "ID") Long id);

  @Operation(summary = "Cambia estado de la moto")
  @PatchMapping("/{id}/status")
  ResponseEntity<java.util.Map<String, String>> changeStatus(
      @PathVariable @Parameter(description = "ID") Long id,
      @RequestBody @Parameter(description = "Nuevo estado") VehicleStatus status);

  @Operation(summary = "Conteo de motos")
  @GetMapping("/count")
  ResponseEntity<java.util.Map<String, Long>> getMotorcycleCounts();

  @Operation(summary = "Busca motos con filtros")
  @GetMapping("/search")
  ResponseEntity<List<MotorcycleResponse>> searchMotorcycles(
      @RequestParam(required = false) @Parameter(description = "Placa") String plate,
      @RequestParam(required = false) @Parameter(description = "Marca") String brand,
      @RequestParam(required = false) @Parameter(description = "Línea") String line,
      @RequestParam(required = false) @Parameter(description = "Modelo") String model,
      @RequestParam(required = false) @Parameter(description = "Tipo de moto")
          String motorcycleType,
      @RequestParam(required = false) @Parameter(description = "Transmisión") String transmission,
      @RequestParam(required = false) @Parameter(description = "Ciudad") String city,
      @RequestParam(required = false) @Parameter(description = "Estado") VehicleStatus status,
      @RequestParam(required = false) @Parameter(description = "Año mínimo") Integer minYear,
      @RequestParam(required = false) @Parameter(description = "Año máximo") Integer maxYear,
      @RequestParam(required = false) @Parameter(description = "Cilindraje mínimo")
          Integer minCapacity,
      @RequestParam(required = false) @Parameter(description = "Cilindraje máximo")
          Integer maxCapacity,
      @RequestParam(required = false) @Parameter(description = "Kilometraje mínimo")
          Integer minMileage,
      @RequestParam(required = false) @Parameter(description = "Kilometraje máximo")
          Integer maxMileage,
      @RequestParam(required = false) @Parameter(description = "Precio mínimo") Double minSalePrice,
      @RequestParam(required = false) @Parameter(description = "Precio máximo")
          Double maxSalePrice);

  @Operation(summary = "Búsqueda paginada de motos")
  @GetMapping("/search/page/{page}")
  ResponseEntity<Page<MotorcycleResponse>> searchMotorcyclesPaginated(
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
      @RequestParam(required = false) Double maxSalePrice);
}
