package com.sgivu.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al confirmar la subida de una imagen de vehículo")
public record VehicleImageConfirmUploadResponse(
    @Schema(description = "ID de la imagen", example = "123") Long imageId,
    @Schema(description = "Bucket", example = "sgivu-vehicles") String bucket,
    @Schema(description = "Key del objeto", example = "images/12345.jpg") String key) {}
