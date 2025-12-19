package com.sgivu.client.mapper;

import com.sgivu.client.dto.CompanyResponse;
import com.sgivu.client.dto.PersonResponse;
import com.sgivu.client.entity.Company;
import com.sgivu.client.entity.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeos entre entidades y DTOs expuestos a otros microservicios SGIVU. Garantiza que se trasladen
 * solo los campos relevantes para contratos, inventario y predicción.
 */
@Mapper(componentModel = "spring")
public interface ClientMapper {

  /**
   * Convierte una empresa a respuesta pública conservando datos fiscales y de contacto.
   *
   * @param company entidad origen
   * @return DTO de respuesta
   */
  @Mapping(source = "id", target = "id")
  @Mapping(source = "address", target = "address")
  @Mapping(source = "phoneNumber", target = "phoneNumber")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "enabled", target = "enabled")
  @Mapping(source = "taxId", target = "taxId")
  @Mapping(source = "companyName", target = "companyName")
  CompanyResponse toCompanyResponse(Company company);

  /**
   * Convierte una persona a DTO de respuesta consumido por contratos y flujos de ventas.
   *
   * @param person entidad origen
   * @return DTO de respuesta
   */
  @Mapping(source = "id", target = "id")
  @Mapping(source = "address", target = "address")
  @Mapping(source = "phoneNumber", target = "phoneNumber")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "enabled", target = "enabled")
  @Mapping(source = "nationalId", target = "nationalId")
  @Mapping(source = "firstName", target = "firstName")
  @Mapping(source = "lastName", target = "lastName")
  PersonResponse toPersonResponse(Person person);
}
