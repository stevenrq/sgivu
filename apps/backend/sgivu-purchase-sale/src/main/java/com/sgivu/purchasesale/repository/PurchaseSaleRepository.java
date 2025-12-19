package com.sgivu.purchasesale.repository;

import com.sgivu.purchasesale.entity.PurchaseSale;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio de contratos de compra/venta. Expone búsquedas por claves de relación para alimentar
 * consultas de inventario y reportes, además de soportar especificaciones dinámicas.
 */
public interface PurchaseSaleRepository
    extends JpaRepository<PurchaseSale, Long>, JpaSpecificationExecutor<PurchaseSale> {

  /**
   * Obtiene los contratos asociados a un cliente específico.
   *
   * @param clientId identificador del cliente (persona o empresa)
   * @return listado de contratos vinculados
   */
  List<PurchaseSale> findByClientId(Long clientId);

  /**
   * Recupera los contratos gestionados por un usuario interno.
   *
   * @param userId identificador del usuario responsable
   * @return lista de contratos relacionados
   */
  List<PurchaseSale> findByUserId(Long userId);

  /**
   * Obtiene el historial de operaciones ligadas a un vehículo.
   *
   * @param vehicleId identificador del vehículo
   * @return contratos que referencian el vehículo
   */
  List<PurchaseSale> findByVehicleId(Long vehicleId);
}
