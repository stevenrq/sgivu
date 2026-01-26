package com.sgivu.vehicle.controller;

import com.sgivu.vehicle.controller.api.MotorcycleApi;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MotorcycleController implements MotorcycleApi {

  private final MotorcycleService motorcycleService;
  private final VehicleMapper vehicleMapper;

  public MotorcycleController(MotorcycleService motorcycleService, VehicleMapper vehicleMapper) {
    this.motorcycleService = motorcycleService;
    this.vehicleMapper = vehicleMapper;
  }

  @Override
  @PreAuthorize("hasAuthority('motorcycle:create')")
  public ResponseEntity<MotorcycleResponse> create(
      Motorcycle motorcycle, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    Motorcycle savedMotorcycle = motorcycleService.save(motorcycle);
    MotorcycleResponse motorcycleResponse = vehicleMapper.toMotorcycleResponse(savedMotorcycle);
    return ResponseEntity.status(HttpStatus.CREATED).body(motorcycleResponse);
  }

  @Override
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<MotorcycleResponse> getById(Long id) {
    return motorcycleService
        .findById(id)
        .map(vehicleMapper::toMotorcycleResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<List<MotorcycleResponse>> getAll() {
    return ResponseEntity.ok(
        motorcycleService.findAll().stream().map(vehicleMapper::toMotorcycleResponse).toList());
  }

  @Override
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<Page<MotorcycleResponse>> getAllPaginated(Integer page) {
    return ResponseEntity.ok(
        motorcycleService
            .findAll(PageRequest.of(page, 10))
            .map(vehicleMapper::toMotorcycleResponse));
  }

  @Override
  @PreAuthorize("hasAuthority('motorcycle:update')")
  public ResponseEntity<MotorcycleResponse> update(
      Long id, Motorcycle motorcycle, BindingResult bindingResult) {
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

  @Override
  @PreAuthorize("hasAuthority('motorcycle:delete')")
  public ResponseEntity<Void> deleteById(Long id) {
    if (motorcycleService.findById(id).isPresent()) {
      motorcycleService.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }

  @Override
  @PreAuthorize("hasAuthority('motorcycle:update')")
  public ResponseEntity<Map<String, String>> changeStatus(Long id, VehicleStatus status) {
    if (motorcycleService.changeStatus(id, status).isPresent()) {
      return ResponseEntity.ok(Collections.singletonMap("status", status.name()));
    }
    return ResponseEntity.notFound().build();
  }

  @Override
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

  @Override
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<List<MotorcycleResponse>> searchMotorcycles(
      String plate,
      String brand,
      String line,
      String model,
      String motorcycleType,
      String transmission,
      String city,
      VehicleStatus status,
      Integer minYear,
      Integer maxYear,
      Integer minCapacity,
      Integer maxCapacity,
      Integer minMileage,
      Integer maxMileage,
      Double minSalePrice,
      Double maxSalePrice) {

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

  @Override
  @PreAuthorize("hasAuthority('motorcycle:read')")
  public ResponseEntity<Page<MotorcycleResponse>> searchMotorcyclesPaginated(
      Integer page,
      Integer size,
      String plate,
      String brand,
      String line,
      String model,
      String motorcycleType,
      String transmission,
      String city,
      VehicleStatus status,
      Integer minYear,
      Integer maxYear,
      Integer minCapacity,
      Integer maxCapacity,
      Integer minMileage,
      Integer maxMileage,
      Double minSalePrice,
      Double maxSalePrice) {

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

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
