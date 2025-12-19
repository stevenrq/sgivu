package com.sgivu.vehicle.dto;

/**
 * Solicitud para generar una URL prefirmada de subida de imagen.
 *
 * <p>El contentType se valida para evitar extensiones no soportadas antes de delegar a S3,
 * protegiendo el pipeline de publicación de vehículos.
 */
public record VehicleImagePresignedUploadRequest(String contentType) {}
