package com.sgivu.vehicle.service;

import com.sgivu.vehicle.dto.VehicleImageConfirmUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadResponse;
import com.sgivu.vehicle.dto.VehicleImageResponse;
import com.sgivu.vehicle.entity.VehicleImage;
import java.util.List;

/**
 * Orquesta la gestión de imágenes de vehículos con S3.
 *
 * <p>Encapsula la generación/validación de URLs prefirmadas y la persistencia de metadatos,
 * asegurando coherencia entre el inventario y el bucket compartido con el frontend.
 *
 * @see com.sgivu.vehicle.service.S3Service
 */
public interface VehicleImageService {

  /**
   * Prepara una URL prefirmada para subir una imagen directamente a S3.
   *
   * @param vehicleId identificador del vehículo en inventario
   * @param request tipo de contenido solicitado
   * @return bucket, key y URL firmada
   * @throws IllegalArgumentException si el tipo de contenido no es soportado
   */
  VehicleImagePresignedUploadResponse createPresignedUploadUrl(
      Long vehicleId, VehicleImagePresignedUploadRequest request);

  /**
   * Confirma que el archivo existe en S3 y registra la imagen para el vehículo.
   *
   * @param vehicleId identificador del vehículo al que pertenece la imagen
   * @param request metadatos enviados desde el frontend tras la subida
   * @return entidad persistida
   * @throws IllegalArgumentException si la key no existe en S3, no pertenece al vehículo o ya hay
   *     duplicados registrados
   */
  VehicleImage confirmUpload(Long vehicleId, VehicleImageConfirmUploadRequest request);

  /**
   * Recupera imágenes de un vehículo generando URLs de descarga temporales.
   *
   * @param vehicleId identificador del vehículo
   * @return listado con URLs prefirmadas y marca de imagen principal
   */
  List<VehicleImageResponse> getImagesByVehicle(Long vehicleId);

  /**
   * Elimina la imagen y su objeto remoto, ajustando la imagen principal si aplica.
   *
   * @param imageId identificador interno de la imagen
   */
  default void deleteImage(Long imageId) {}
}
