package com.sgivu.vehicle.dto;

/**
 * Payload recibido tras subir la imagen a S3.
 *
 * <p>Permite validar que el archivo exista, no se duplique y, si aplica, marcarlo como imagen
 * principal del vehículo en los portales de venta.
 *
 * @apiNote La bandera {@code primary} habilita que la primera imagen suba como principal sin
 *     requerir operación adicional.
 */
public record VehicleImageConfirmUploadRequest(
    String fileName, String contentType, Long size, String key, Boolean primary) {}
