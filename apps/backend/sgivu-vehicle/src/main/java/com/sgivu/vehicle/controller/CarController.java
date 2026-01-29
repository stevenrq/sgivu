package com.sgivu.vehicle.controller;

import com.sgivu.vehicle.controller.api.CarApi;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CarController implements CarApi {

  private final CarService carService;
  private final VehicleMapper vehicleMapper;

  public CarController(CarService carService, VehicleMapper vehicleMapper) {
    this.carService = carService;
    this.vehicleMapper = vehicleMapper;
  }

  @Override
  @PreAuthorize("hasAuthority('car:create')")
  public ResponseEntity<CarResponse> create(Car car, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    CarResponse carResponse = vehicleMapper.toCarResponse(carService.save(car));
    return ResponseEntity.status(HttpStatus.CREATED).body(carResponse);
  }

  @Override
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<CarResponse> getById(Long id) {
    return carService
        .findById(id)
        .map(car -> ResponseEntity.ok(vehicleMapper.toCarResponse(car)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<List<CarResponse>> getAll() {
    List<CarResponse> carResponses =
        carService.findAll().stream().map(vehicleMapper::toCarResponse).toList();
    return ResponseEntity.ok(carResponses);
  }

  @Override
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<Page<CarResponse>> getAllPaginated(Integer page) {
    return ResponseEntity.ok(
        carService.findAll(PageRequest.of(page, 10)).map(vehicleMapper::toCarResponse));
  }

  @Override
  @PreAuthorize("hasAuthority('car:update')")
  public ResponseEntity<CarResponse> update(Long id, Car car, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    return carService
        .update(id, car)
        .map(updatedCar -> ResponseEntity.ok(vehicleMapper.toCarResponse(updatedCar)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('car:delete')")
  public ResponseEntity<Void> deleteById(Long id) {
    Optional<Car> carOptional = carService.findById(id);

    if (carOptional.isPresent()) {
      carService.deleteById(id);
      return ResponseEntity.noContent().build();
    }

    return ResponseEntity.notFound().build();
  }

  @Override
  @PreAuthorize("hasAuthority('car:update')")
  public ResponseEntity<Map<String, String>> changeStatus(Long id, VehicleStatus status) {
    if (carService.changeStatus(id, status).isPresent()) {
      return ResponseEntity.ok(Collections.singletonMap("status", status.name()));
    }
    return ResponseEntity.notFound().build();
  }

  @Override
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

  @Override
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<List<CarResponse>> searchCars(
      String plate,
      String brand,
      String line,
      String model,
      String fuelType,
      String bodyType,
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

  @Override
  @PreAuthorize("hasAuthority('car:read')")
  public ResponseEntity<Page<CarResponse>> searchCarsPaginated(
      Integer page,
      Integer size,
      String plate,
      String brand,
      String line,
      String model,
      String fuelType,
      String bodyType,
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

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
