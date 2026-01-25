package com.sgivu.vehicle.dto;

/**
 * Solicitud para generar una URL prefirmada de subida de imagen.
 *
 * <p>El contentType se valida para evitar extensiones no soportadas antes de delegar a S3,
 * protegiendo el pipeline de publicación de vehículos.
 */
public record VehicleImagePresignedUploadRequest(
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Tipo de contenido a subir",
            example = "image/jpeg",
            requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
        String contentType) {}
