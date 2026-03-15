package com.sgivu.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta con URL prefirmada para subir una imagen de vehículo")
public record VehicleImagePresignedUploadResponse(
    @Schema(description = "Bucket de destino", example = "sgivu-vehicles") String bucket,
    @Schema(description = "Key del objeto", example = "images/12345.jpg") String key,
    @Schema(description = "URL para subir", example = "https://...") String uploadUrl) {}
