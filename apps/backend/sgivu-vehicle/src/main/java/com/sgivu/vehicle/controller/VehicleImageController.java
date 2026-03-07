package com.sgivu.vehicle.controller;

import com.sgivu.vehicle.controller.api.VehicleImageApi;
import com.sgivu.vehicle.dto.*;
import com.sgivu.vehicle.mapper.VehicleMapper;
import com.sgivu.vehicle.service.VehicleImageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VehicleImageController implements VehicleImageApi {

  private final VehicleImageService vehicleImageService;
  private final VehicleMapper vehicleMapper;

  public VehicleImageController(
      VehicleImageService vehicleImageService, VehicleMapper vehicleMapper) {
    this.vehicleImageService = vehicleImageService;
    this.vehicleMapper = vehicleMapper;
  }

  @Override
  @PreAuthorize("hasAuthority('vehicle:create')")
  public ResponseEntity<VehicleImagePresignedUploadResponse> createPresignedUploadUrl(
      Long vehicleId, VehicleImagePresignedUploadRequest request) {

    return ResponseEntity.ok(vehicleImageService.createPresignedUploadUrl(vehicleId, request));
  }

  @Override
  @PreAuthorize("hasAuthority('vehicle:create')")
  public ResponseEntity<VehicleImageConfirmUploadResponse> confirmUpload(
      Long vehicleId, VehicleImageConfirmUploadRequest request) {
    return ResponseEntity.ok(
        vehicleMapper.toVehicleImageConfirmUploadResponse(
            vehicleImageService.confirmUpload(vehicleId, request)));
  }

  @Override
  @PreAuthorize("hasAuthority('vehicle:read')")
  public ResponseEntity<List<VehicleImageResponse>> getImages(Long vehicleId) {
    return ResponseEntity.ok(vehicleImageService.getImagesByVehicle(vehicleId));
  }

  @Override
  @PreAuthorize("hasAuthority('vehicle:delete')")
  public ResponseEntity<Void> deleteImage(Long vehicleId, Long imageId) {
    List<VehicleImageResponse> images = vehicleImageService.getImagesByVehicle(vehicleId);
    if (images == null || images.stream().noneMatch(img -> img.id().equals(imageId))) {
      return ResponseEntity.notFound().build();
    }
    vehicleImageService.deleteImage(imageId);
    return ResponseEntity.noContent().build();
  }
}
