package com.sgivu.vehicle.mapper;

import com.sgivu.vehicle.dto.CarResponse;
import com.sgivu.vehicle.dto.MotorcycleResponse;
import com.sgivu.vehicle.dto.VehicleImageConfirmUploadResponse;
import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.entity.Motorcycle;
import com.sgivu.vehicle.entity.VehicleImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

  CarResponse toCarResponse(Car car);

  MotorcycleResponse toMotorcycleResponse(Motorcycle motorcycle);

  @Mapping(source = "id", target = "imageId")
  VehicleImageConfirmUploadResponse toVehicleImageConfirmUploadResponse(VehicleImage vehicleImage);
}
