package com.sgivu.purchasesale.mapper;

import com.sgivu.purchasesale.dto.ContractStatusHistoryResponse;
import com.sgivu.purchasesale.entity.ContractStatusHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContractStatusHistoryMapper {
  ContractStatusHistoryResponse toResponse(ContractStatusHistory entity);
}
