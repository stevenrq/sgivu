package com.sgivu.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para confirmar la subida de una imagen de vehículo")
public record VehicleImageConfirmUploadRequest(
    @Schema(description = "Nombre del archivo", example = "front.jpg") String fileName,
    @Schema(description = "Tipo de contenido", example = "image/jpeg") String contentType,
    @Schema(description = "Tamaño en bytes", example = "12345") Long size,
    @Schema(description = "Key de S3", example = "images/12345.jpg") String key,
    @Schema(description = "Indica si es principal", example = "true") Boolean primary) {}
