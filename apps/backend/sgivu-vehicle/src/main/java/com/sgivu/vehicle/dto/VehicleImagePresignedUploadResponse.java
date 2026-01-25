package com.sgivu.vehicle.dto;

/**
 * Respuesta con la información necesaria para que el frontend suba la imagen.
 *
 * <p>Incluye bucket y key firmados que luego serán confirmados para registrar la imagen en el
 * inventario.
 */
public record VehicleImagePresignedUploadResponse(
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Bucket de destino",
            example = "sgivu-vehicles")
        String bucket,
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Key del objeto",
            example = "images/12345.jpg")
        String key,
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "URL para subir",
            example = "https://...")
        String uploadUrl) {}
