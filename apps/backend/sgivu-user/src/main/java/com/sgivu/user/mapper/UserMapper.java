package com.sgivu.user.mapper;

import com.sgivu.user.dto.UserResponse;
import com.sgivu.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
  UserResponse toUserResponse(User user);
}
