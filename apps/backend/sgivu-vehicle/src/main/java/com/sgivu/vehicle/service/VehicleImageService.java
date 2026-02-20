package com.sgivu.vehicle.service;

import com.sgivu.vehicle.dto.VehicleImageConfirmUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadResponse;
import com.sgivu.vehicle.dto.VehicleImageResponse;
import com.sgivu.vehicle.entity.VehicleImage;
import java.util.List;

public interface VehicleImageService {

  VehicleImagePresignedUploadResponse createPresignedUploadUrl(
      Long vehicleId, VehicleImagePresignedUploadRequest request);

  VehicleImage confirmUpload(Long vehicleId, VehicleImageConfirmUploadRequest request);

  List<VehicleImageResponse> getImagesByVehicle(Long vehicleId);

  default void deleteImage(Long imageId) {}
}
