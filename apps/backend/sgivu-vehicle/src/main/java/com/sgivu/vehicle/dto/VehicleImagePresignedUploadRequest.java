package com.sgivu.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para obtener una URL prefirmada para subir una imagen de vehículo")
public record VehicleImagePresignedUploadRequest(
    @Schema(
            description = "Tipo de contenido a subir",
            example = "image/jpeg",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String contentType) {}
