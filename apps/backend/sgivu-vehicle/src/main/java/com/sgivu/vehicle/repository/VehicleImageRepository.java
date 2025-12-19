package com.sgivu.vehicle.repository;

import com.sgivu.vehicle.entity.VehicleImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de metadatos de imágenes de vehículos.
 *
 * <p>Permite obtener y validar imágenes por vehículo antes de operar con S3, asegurando
 * consistencia entre el bucket y la base de datos.
 */
public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {

  /**
   * Obtiene todas las imágenes asociadas a un vehículo específico, ordenándolas primero por si son
   * imagen principal (descendente) y luego por la fecha de creación (ascendente).
   *
   * @param vehicleId identificador del vehículo cuyas imágenes se desean consultar
   * @return lista de {@link VehicleImage} con la imagen principal al inicio, seguida del resto
   *     ordenadas por fecha de creación
   */
  List<VehicleImage> findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(Long vehicleId);

  boolean existsByVehicleIdAndFileName(Long vehicleId, String fileName);

  boolean existsByKey(String key);
}
