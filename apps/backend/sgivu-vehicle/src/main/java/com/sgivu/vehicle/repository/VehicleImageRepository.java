package com.sgivu.vehicle.repository;

import com.sgivu.vehicle.entity.VehicleImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {

  List<VehicleImage> findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(Long vehicleId);

  boolean existsByVehicleIdAndFileName(Long vehicleId, String fileName);

  boolean existsByKey(String key);
}
