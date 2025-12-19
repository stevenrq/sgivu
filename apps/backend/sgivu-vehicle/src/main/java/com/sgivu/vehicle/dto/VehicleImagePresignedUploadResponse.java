package com.sgivu.vehicle.dto;

/**
 * Respuesta con la información necesaria para que el frontend suba la imagen.
 *
 * <p>Incluye bucket y key firmados que luego serán confirmados para registrar la imagen en el
 * inventario.
 */
public record VehicleImagePresignedUploadResponse(String bucket, String key, String uploadUrl) {}
