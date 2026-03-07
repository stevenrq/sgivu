package com.sgivu.purchasesale.client;

import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.Motorcycle;
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

  @PostExchange("/cars")
  Car createCar(@RequestBody Car car);

  @PostExchange("/motorcycles")
  Motorcycle createMotorcycle(@RequestBody Motorcycle motorcycle);
}
