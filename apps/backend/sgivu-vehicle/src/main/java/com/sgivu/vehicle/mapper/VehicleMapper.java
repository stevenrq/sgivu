package com.sgivu.vehicle.mapper;

import com.sgivu.vehicle.dto.CarResponse;
import com.sgivu.vehicle.dto.MotorcycleResponse;
import com.sgivu.vehicle.dto.VehicleImageConfirmUploadResponse;
import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.entity.Motorcycle;
import com.sgivu.vehicle.entity.VehicleImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeador entre entidades y DTOs expuestos por la API.
 *
 * <p>Aísla conversiones de la capa web para evitar fugas de entidades JPA y controlar qué datos se
 * comparten con otros microservicios o el frontend.
 */
@Mapper(componentModel = "spring")
public interface VehicleMapper {

  /**
   * Convierte entidad {@link Car} a DTO listo para exposición pública.
   *
   * @param car entidad JPA
   * @return DTO con datos de negocio
   */
  CarResponse toCarResponse(Car car);

  /**
   * Convierte entidad {@link Motorcycle} a DTO público.
   *
   * @param motorcycle entidad JPA
   * @return DTO de respuesta
   */
  MotorcycleResponse toMotorcycleResponse(Motorcycle motorcycle);

  /**
   * Devuelve la respuesta de confirmación usando el id persistido.
   *
   * @param vehicleImage entidad almacenada tras validar la subida
   * @return DTO usado por el frontend para futuras operaciones
   */
  @Mapping(source = "id", target = "imageId")
  VehicleImageConfirmUploadResponse toVehicleImageConfirmUploadResponse(VehicleImage vehicleImage);
}
