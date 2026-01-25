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
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Nombre del archivo",
            example = "front.jpg")
        String fileName,
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Tipo de contenido",
            example = "image/jpeg")
        String contentType,
    @io.swagger.v3.oas.annotations.media.Schema(description = "Tamaño en bytes", example = "12345")
        Long size,
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Key de S3",
            example = "images/12345.jpg")
        String key,
    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Indica si es principal",
            example = "true")
        Boolean primary) {}
