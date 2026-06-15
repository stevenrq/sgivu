package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.Motorcycle;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.Vehicle;
import com.sgivu.purchasesale.dto.VehicleCreationRequest;
import com.sgivu.purchasesale.enums.VehicleType;
import com.sgivu.purchasesale.exception.VehicleRegistrationException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Servicio encargado de registrar vehículos nuevos en el microservicio de inventario durante el
 * flujo de compra. Un contrato de compra puede crear un vehículo como efecto secundario cuando éste
 * no existe previamente en inventario. La validación de campos se realiza aquí y no mediante Bean
 * Validation porque los campos requeridos varían según el tipo de vehículo (auto requiere
 * carrocería, combustible y puertas; motocicleta requiere tipo de motocicleta) y porque el precio
 * de compra puede provenir del contrato o del bloque vehicleData.
 */
@Service
public class VehicleRegistrationService {

  /** Estado inicial asignado a todo vehículo registrado durante una compra. */
  static final String INITIAL_VEHICLE_STATUS = "AVAILABLE";

  private final VehicleServiceClient vehicleServiceClient;

  public VehicleRegistrationService(VehicleServiceClient vehicleServiceClient) {
    this.vehicleServiceClient = vehicleServiceClient;
  }

  /**
   * Registra un vehículo nuevo (auto o motocicleta) en el microservicio de inventario a partir de
   * los datos del request de compra.
   *
   * @param purchaseSaleRequest request que contiene los datos del vehículo en {@code vehicleData}
   * @return el ID del vehículo recién creado en el microservicio de inventario
   * @throws VehicleRegistrationException si faltan datos obligatorios del vehículo
   */
  public Long registerVehicle(PurchaseSaleRequest purchaseSaleRequest) {
    VehicleCreationRequest vehicleData = purchaseSaleRequest.getVehicleData();
    if (vehicleData == null) {
      throw new VehicleRegistrationException(
          "Debes proporcionar los datos del vehículo para registrar una compra.");
    }

    VehicleType vehicleType =
        Optional.ofNullable(vehicleData.getVehicleType())
            .orElseThrow(
                () ->
                    new VehicleRegistrationException(
                        "Debes especificar si se trata de un automóvil o una motocicleta."));

    if (vehicleType == VehicleType.CAR) {
      return buildAndCreateCar(vehicleData, purchaseSaleRequest);
    }
    return buildAndCreateMotorcycle(vehicleData, purchaseSaleRequest);
  }

  private Long buildAndCreateCar(VehicleCreationRequest vehicleData, PurchaseSaleRequest request) {
    Car car = applyCommonAttributes(new Car(), vehicleData, request);
    car.setBodyType(
        requireText(vehicleData.getBodyType(), "La carrocería del automóvil es obligatoria."));
    car.setFuelType(
        requireText(
            vehicleData.getFuelType(), "El tipo de combustible del automóvil es obligatorio."));
    car.setNumberOfDoors(
        requirePositiveInteger(
            vehicleData.getNumberOfDoors(), "Debes indicar el número de puertas del automóvil."));
    return vehicleServiceClient.createCar(car).getId();
  }

  private Long buildAndCreateMotorcycle(
      VehicleCreationRequest vehicleData, PurchaseSaleRequest request) {
    Motorcycle motorcycle = applyCommonAttributes(new Motorcycle(), vehicleData, request);
    motorcycle.setMotorcycleType(
        requireText(vehicleData.getMotorcycleType(), "Debes indicar el tipo de motocicleta."));
    return vehicleServiceClient.createMotorcycle(motorcycle).getId();
  }

  /**
   * Aplica los atributos comunes a cualquier tipo de vehículo. El precio de compra puede venir del
   * bloque vehicleData o del contrato como fallback, porque el usuario puede proporcionar el precio
   * en cualquiera de los dos niveles del request.
   *
   * @param target vehículo a construir (auto o motocicleta)
   * @param vehicleData datos del vehículo provenientes del request
   * @param request request completo del contrato, utilizado para resolver el precio de compra como
   *     fallback
   */
  private <T extends Vehicle> T applyCommonAttributes(
      T target, VehicleCreationRequest vehicleData, PurchaseSaleRequest request) {
    target.setBrand(requireText(vehicleData.getBrand(), "La marca del vehículo es obligatoria."));
    target.setModel(requireText(vehicleData.getModel(), "El modelo del vehículo es obligatorio."));
    target.setCapacity(
        requirePositiveInteger(
            vehicleData.getCapacity(), "La capacidad de pasajeros del vehículo es obligatoria."));
    target.setLine(requireText(vehicleData.getLine(), "La línea del vehículo es obligatoria."));
    target.setPlate(
        requireText(vehicleData.getPlate(), "La placa del vehículo es obligatoria.").toUpperCase());
    target.setMotorNumber(
        requireText(
            vehicleData.getMotorNumber(), "El número de motor del vehículo es obligatorio."));
    target.setSerialNumber(
        requireText(
            vehicleData.getSerialNumber(), "El número serial del vehículo es obligatorio."));
    target.setChassisNumber(
        requireText(
            vehicleData.getChassisNumber(), "El número de chasis del vehículo es obligatorio."));
    target.setColor(requireText(vehicleData.getColor(), "El color del vehículo es obligatorio."));
    target.setCityRegistered(
        requireText(
            vehicleData.getCityRegistered(),
            "La ciudad de matrícula del vehículo es obligatoria."));
    target.setYear(requireValidYear(vehicleData.getYear()));
    target.setMileage(requireNonNegativeInteger(vehicleData.getMileage()));
    target.setTransmission(
        requireText(vehicleData.getTransmission(), "La transmisión del vehículo es obligatoria."));
    target.setPurchasePrice(resolveVehiclePurchasePrice(vehicleData, request.getPurchasePrice()));
    target.setSalePrice(resolveVehicleSalePrice(vehicleData.getSalePrice()));
    target.setPhotoUrl(normalizeNullable(vehicleData.getPhotoUrl()));
    target.setStatus(INITIAL_VEHICLE_STATUS);
    return target;
  }

  // === Helpers de validación de campos de vehículo ===

  String requireText(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new VehicleRegistrationException(message);
    }
    return value.trim();
  }

  Integer requirePositiveInteger(Integer value, String message) {
    if (value == null || value <= 0) {
      throw new VehicleRegistrationException(message);
    }
    return value;
  }

  Integer requireNonNegativeInteger(Integer value) {
    if (value == null || value < 0) {
      throw new VehicleRegistrationException("El kilometraje del vehículo es obligatorio.");
    }
    return value;
  }

  /**
   * El rango 1950-2050 existe porque la empresa no comercializa vehículos clásicos anteriores a
   * 1950 y se establece 2050 como límite superior razonable.
   *
   * @param value año a validar
   * @return el año validado
   * @throws VehicleRegistrationException si el año es nulo o está fuera del rango 1950-2050
   */
  Integer requireValidYear(Integer value) {
    Integer year = requirePositiveInteger(value, "El año del vehículo es obligatorio.");
    if (year < 1950 || year > 2050) {
      throw new VehicleRegistrationException("El año del vehículo debe estar entre 1950 y 2050.");
    }
    return year;
  }

  /**
   * Resuelve el precio de compra del vehículo: primero intenta vehicleData, luego el precio del
   * contrato como fallback. Ambas fuentes son válidas porque el usuario puede especificarlo en
   * cualquiera de los dos niveles del request.
   *
   * @param vehicleData datos del vehículo que pueden contener purchasePrice
   * @param fallback precio de compra proporcionado en el contrato, utilizado si vehicleData no lo
   *     incluye
   * @return el precio de compra a asignar al vehículo
   * @throws VehicleRegistrationException si el precio de compra es nulo o no positivo en ninguna de
   *     las dos fuentes
   */
  Double resolveVehiclePurchasePrice(VehicleCreationRequest vehicleData, Double fallback) {
    Double value =
        vehicleData.getPurchasePrice() != null ? vehicleData.getPurchasePrice() : fallback;
    if (value == null || value <= 0) {
      throw new VehicleRegistrationException(
          "El precio de compra del vehículo debe ser mayor a cero.");
    }
    return value;
  }

  /**
   * El precio de venta del vehículo es opcional en compras (se puede definir después). Un valor
   * nulo se normaliza a 0 para mantener la columna NOT NULL de la entidad.
   *
   * @param salePrice precio de venta proporcionado en vehicleData, que es opcional para compras
   * @return el precio de venta a asignar al vehículo, normalizado a 0 si es nulo
   * @throws VehicleRegistrationException si el precio de venta es negativo
   */
  Double resolveVehicleSalePrice(Double salePrice) {
    if (salePrice == null) {
      return 0d;
    }
    if (salePrice < 0) {
      throw new VehicleRegistrationException(
          "El precio de venta del vehículo no puede ser negativo.");
    }
    return salePrice;
  }

  private String normalizeNullable(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
