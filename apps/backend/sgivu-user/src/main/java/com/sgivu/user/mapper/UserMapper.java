package com.sgivu.user.mapper;

import com.sgivu.user.dto.UserResponse;
import com.sgivu.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Traduce entidades {@link User} a {@link UserResponse} evitando exponer detalles internos de JPA.
 *
 * <p>El mapeo explícito facilita auditar qué campos son devueltos al Authorization Server y a los
 * paneles de administración.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(source = "id", target = "id")
  @Mapping(source = "nationalId", target = "nationalId")
  @Mapping(source = "firstName", target = "firstName")
  @Mapping(source = "lastName", target = "lastName")
  @Mapping(source = "address", target = "address")
  @Mapping(source = "phoneNumber", target = "phoneNumber")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "password", target = "password")
  @Mapping(source = "enabled", target = "enabled")
  @Mapping(source = "accountNonExpired", target = "accountNonExpired")
  @Mapping(source = "accountNonLocked", target = "accountNonLocked")
  @Mapping(source = "credentialsNonExpired", target = "credentialsNonExpired")
  @Mapping(source = "roles", target = "roles")
  /**
   * Proyección hacia {@link UserResponse} para evitar exponer proxies y garantizar consistencia de
   * campos retornados a otros servicios.
   *
   * @param user entidad de base de datos.
   * @return DTO listo para serializar.
   */
  UserResponse toUserResponse(User user);
}
