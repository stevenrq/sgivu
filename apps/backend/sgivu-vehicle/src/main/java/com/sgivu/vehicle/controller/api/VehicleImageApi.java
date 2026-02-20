package com.sgivu.vehicle.controller.api;

import com.sgivu.vehicle.dto.VehicleImageConfirmUploadRequest;
import com.sgivu.vehicle.dto.VehicleImageConfirmUploadResponse;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadResponse;
import com.sgivu.vehicle.dto.VehicleImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Imágenes de Vehículos",
    description = "Operaciones para subir, confirmar y listar imágenes")
@RequestMapping("/v1/vehicles/{vehicleId}/images")
public interface VehicleImageApi {

  @Operation(summary = "Genera URL prefirmada para subir imagen")
  @PostMapping("/presigned-upload")
  ResponseEntity<VehicleImagePresignedUploadResponse> createPresignedUploadUrl(
      @PathVariable @Parameter(description = "ID del vehículo") Long vehicleId,
      @Valid @RequestBody @Parameter(description = "Solicitud con tipo de archivo")
          VehicleImagePresignedUploadRequest request);

  @Operation(summary = "Confirma subida y registra metadatos")
  @PostMapping("/confirm-upload")
  ResponseEntity<VehicleImageConfirmUploadResponse> confirmUpload(
      @PathVariable @Parameter(description = "ID del vehículo") Long vehicleId,
      @Valid @RequestBody @Parameter(description = "Datos de la imagen subida")
          VehicleImageConfirmUploadRequest request);

  @Operation(summary = "Lista imágenes de un vehículo")
  @GetMapping
  ResponseEntity<List<VehicleImageResponse>> getImages(
      @PathVariable @Parameter(description = "ID del vehículo") Long vehicleId);

  @Operation(summary = "Elimina imagen")
  @DeleteMapping("/{imageId}")
  ResponseEntity<Void> deleteImage(
      @PathVariable @Parameter(description = "ID del vehículo") Long vehicleId,
      @PathVariable @Parameter(description = "ID de la imagen") Long imageId);
}
