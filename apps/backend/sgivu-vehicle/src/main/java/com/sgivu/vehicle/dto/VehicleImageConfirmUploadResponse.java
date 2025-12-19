package com.sgivu.vehicle.dto;

/**
 * Respuesta después de persistir los metadatos de una imagen.
 *
 * <p>Retorna el identificador interno necesario para futuras operaciones (eliminar, actualizar
 * imagen principal) y la ubicación del objeto en S3.
 */
public record VehicleImageConfirmUploadResponse(Long imageId, String bucket, String key) {}
