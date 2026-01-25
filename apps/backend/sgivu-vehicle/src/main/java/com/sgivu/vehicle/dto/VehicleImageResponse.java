package com.sgivu.vehicle.dto;

/**
 * DTO expuesto al frontend con URLs temporales de descarga.
 *
 * <p>La URL es prefirmada y caduca para proteger el bucket; {@code primary} indica qué imagen se
 * prioriza en vitrinas de venta.
 */
public record VehicleImageResponse(
    @io.swagger.v3.oas.annotations.media.Schema(description = "ID de la imagen", example = "123")
        Long id,
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "URL prefirmada",
            example = "https://...")
        String url,
    @io.swagger.v3.oas.annotations.media.Schema(description = "Indica principal", example = "true")
        boolean primary) {}
