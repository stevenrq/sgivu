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

@Mapper(componentModel = "spring")
public interface PurchaseSaleMapper {

  PurchaseSaleResponse toPurchaseSaleResponse(PurchaseSale purchaseSale);

  @Mapping(target = "clientSummary", ignore = true)
  @Mapping(target = "userSummary", ignore = true)
  @Mapping(target = "vehicleSummary", ignore = true)
  PurchaseSaleDetailResponse toPurchaseSaleDetailResponse(PurchaseSale purchaseSale);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  PurchaseSale toPurchaseSale(PurchaseSaleRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updatePurchaseSaleFromRequest(
      PurchaseSaleRequest request, @MappingTarget PurchaseSale purchaseSale);
}
