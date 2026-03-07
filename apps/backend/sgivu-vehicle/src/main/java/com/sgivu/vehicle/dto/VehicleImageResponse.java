package com.sgivu.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta con información de una imagen de vehículo")
public record VehicleImageResponse(
    @Schema(description = "ID de la imagen", example = "123") Long id,
    @Schema(description = "URL prefirmada", example = "https://...") String url,
    @Schema(description = "Indica principal", example = "true") boolean primary) {}
