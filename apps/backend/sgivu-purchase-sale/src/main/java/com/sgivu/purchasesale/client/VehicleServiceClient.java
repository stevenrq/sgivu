package com.sgivu.purchasesale.client;

import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.Motorcycle;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Cliente declarativo del microservicio de vehículos. Se reutiliza tanto para consultar
 * vehículos existentes (durante ventas) como para registrar nuevos (durante compras).
 */
@HttpExchange("/v1")
public interface VehicleServiceClient {

  /**
   * Recupera un automóvil previamente registrado, utilizado para validar disponibilidad en
   * contratos de venta.
   *
   * @param id identificador del automóvil
   * @return datos del vehículo
   */
  @GetExchange("/cars/{id}")
  Car getCarById(@PathVariable Long id);

  /**
   * Recupera una motocicleta previamente registrada.
   *
   * @param id identificador de la motocicleta
   * @return datos del vehículo
   */
  @GetExchange("/motorcycles/{id}")
  Motorcycle getMotorcycleById(@PathVariable Long id);

  /**
   * Registra un automóvil adquirido; la creación se dispara solo en contratos de compra para no
   * duplicar inventario.
   *
   * @param car datos completos del automóvil a persistir en inventario
   * @return vehículo creado con su identificador
   */
  @PostExchange("/cars")
  Car createCar(@RequestBody Car car);

  /**
   * Registra una motocicleta adquirida.
   *
   * @param motorcycle datos de la motocicleta a registrar
   * @return vehículo creado con su identificador
   */
  @PostExchange("/motorcycles")
  Motorcycle createMotorcycle(@RequestBody Motorcycle motorcycle);
}
