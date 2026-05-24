package com.sgivu.purchasesale.client;

import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.Motorcycle;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/v1")
public interface VehicleServiceClient {

  @GetExchange("/cars/{id}")
  Car getCarById(@PathVariable Long id);

  @GetExchange("/motorcycles/{id}")
  Motorcycle getMotorcycleById(@PathVariable Long id);

  @PostExchange("/cars/batch")
  List<Car> getCarsByIds(@RequestBody List<Long> ids);

  @PostExchange("/motorcycles/batch")
  List<Motorcycle> getMotorcyclesByIds(@RequestBody List<Long> ids);

  @GetExchange("/cars/count")
  Map<String, Long> getCarCounts();

  @GetExchange("/motorcycles/count")
  Map<String, Long> getMotorcycleCounts();

  @PostExchange("/cars")
  Car createCar(@RequestBody Car car);

  @PostExchange("/motorcycles")
  Motorcycle createMotorcycle(@RequestBody Motorcycle motorcycle);
}
