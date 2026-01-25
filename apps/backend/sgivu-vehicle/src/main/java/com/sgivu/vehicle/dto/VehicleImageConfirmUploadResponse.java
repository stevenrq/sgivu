package com.sgivu.vehicle.dto;

/**
 * Respuesta después de persistir los metadatos de una imagen.
 *
 * <p>Retorna el identificador interno necesario para futuras operaciones (eliminar, actualizar
 * imagen principal) y la ubicación del objeto en S3.
 */
public record VehicleImageConfirmUploadResponse(
    @io.swagger.v3.oas.annotations.media.Schema(description = "ID de la imagen", example = "123")
        Long imageId,
    @io.swagger.v3.oas.annotations.media.Schema(description = "Bucket", example = "sgivu-vehicles")
        String bucket,
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Key del objeto",
            example = "images/12345.jpg")
        String key) {}
