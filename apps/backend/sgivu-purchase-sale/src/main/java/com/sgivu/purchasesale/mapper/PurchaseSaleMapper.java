package com.sgivu.purchasesale.mapper;

import com.sgivu.purchasesale.dto.PurchaseSaleDetailResponse;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.PurchaseSaleResponse;
import com.sgivu.purchasesale.entity.PurchaseSale;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper centralizado para transformar contratos entre entidad y DTOs. Separa la persistencia de
 * los contratos del formato de intercambio utilizado por el API y los reportes, permitiendo
 * incorporar resúmenes externos de clientes/usuarios/vehículos sin contaminar la entidad JPA.
 */
@Mapper(componentModel = "spring")
public interface PurchaseSaleMapper {

  /** Convierte la entidad a su representación básica para listados rápidos. */
  PurchaseSaleResponse toPurchaseSaleResponse(PurchaseSale purchaseSale);

  /**
   * Transforma la entidad a un DTO listo para enriquecer con datos externos de cliente, usuario y
   * vehículo.
   *
   * @apiNote El enriquecimiento ocurre en {@code PurchaseSaleDetailService} para mantener bajo
   *     acoplamiento con clientes HTTP.
   */
  @Mapping(target = "clientSummary", ignore = true)
  @Mapping(target = "userSummary", ignore = true)
  @Mapping(target = "vehicleSummary", ignore = true)
  PurchaseSaleDetailResponse toPurchaseSaleDetailResponse(PurchaseSale purchaseSale);

  /**
   * Construye la entidad a partir del request API sin sobreescribir campos administrados por la
   * base de datos.
   *
   * @param request payload recibido
   * @return entidad lista para persistir
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  PurchaseSale toPurchaseSale(PurchaseSaleRequest request);

  /**
   * Aplica cambios parciales desde el request hacia la entidad existente sin tocar campos nulos ni
   * columnas manejadas por triggers/lifecycle.
   *
   * @param request datos entrantes
   * @param purchaseSale entidad persistida a actualizar
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updatePurchaseSaleFromRequest(
      PurchaseSaleRequest request, @MappingTarget PurchaseSale purchaseSale);
}
