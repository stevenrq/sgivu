package com.sgivu.vehicle.dto;

/**
 * DTO expuesto al frontend con URLs temporales de descarga.
 *
 * <p>La URL es prefirmada y caduca para proteger el bucket; {@code primary} indica qué imagen se
 * prioriza en vitrinas de venta.
 */
public record VehicleImageResponse(Long id, String url, boolean primary) {}
