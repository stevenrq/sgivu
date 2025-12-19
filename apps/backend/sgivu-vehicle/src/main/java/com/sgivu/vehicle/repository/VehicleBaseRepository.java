package com.sgivu.vehicle.repository;

import com.sgivu.vehicle.entity.Vehicle;

/**
 * Repositorio base para operaciones polimórficas sobre {@link Vehicle}.
 *
 * <p>Se utiliza cuando no importa el subtipo (auto/moto), por ejemplo, para confirmar imágenes o
 * validar unicidad de placas a nivel global del inventario.
 */
public interface VehicleBaseRepository extends VehicleRepository<Vehicle> {}
