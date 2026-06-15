package com.sgivu.user.util;

import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.Permission;
import com.sgivu.user.entity.Role;
import com.sgivu.user.entity.User;
import com.sgivu.user.exception.RoleRetrievalException;
import com.sgivu.user.repository.RoleRepository;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RolePermissionUtils {
  private static final String USER = "USER";

  private RolePermissionUtils() {}

  public static Set<String> getRolesAndPermissions(Set<Role> roles) {
    Objects.requireNonNull(roles, "El conjunto de roles no debe ser nulo");

    return Stream.concat(
            roles.stream().map(Role::getName),
            roles.stream().flatMap(role -> role.getPermissions().stream()).map(Permission::getName))
        .collect(Collectors.toSet());
  }

  public static Set<Role> getRoles(Object user, RoleRepository roleRepository) {
    Set<Role> roles = new HashSet<>();

    if (user instanceof User u) {
      roles = getRolesFromUser(u, roleRepository);
    } else if (user instanceof UserUpdateRequest ur) {
      roles = getRolesFromUserUpdateRequest(ur, roleRepository);
    }
    return roles;
  }

  private static Set<Role> getRolesFromUser(User user, RoleRepository roleRepository) {
    Set<Role> roles = new HashSet<>();

    try {
      Set<String> rolesAndPermissionsOfUser = user.getRolesAndPermissions();

      if (rolesAndPermissionsOfUser.isEmpty()) {
        roles.add(roleRepository.findByName(USER).orElseThrow());
      } else {
        for (String role : rolesAndPermissionsOfUser) {
          Optional<Role> roleOptional = roleRepository.findByName(role);
          roleOptional.ifPresent(roles::add);
        }
      }
    } catch (NoSuchElementException e) {
      throw new RoleRetrievalException("Error retrieving roles: " + e.getMessage(), e);
    } catch (IllegalArgumentException e) {
      throw new RoleRetrievalException("Invalid role name: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RoleRetrievalException("Unexpected error occurred: " + e.getMessage(), e);
    }
    return roles;
  }

  private static Set<Role> getRolesFromUserUpdateRequest(
      UserUpdateRequest userUpdateRequest, RoleRepository roleRepository) {

    Set<Role> roles = new HashSet<>();

    try {
      Set<String> rolesAndPermissionsOfUserUpdateRequest =
          userUpdateRequest.getRolesAndPermissions();

      if (!rolesAndPermissionsOfUserUpdateRequest.isEmpty()) {
        for (String role : rolesAndPermissionsOfUserUpdateRequest) {
          Optional<Role> roleOptional = roleRepository.findByName(role);
          roleOptional.ifPresent(roles::add);
        }
      } else {
        // En actualizaciones exigimos roles explícitos para evitar desproteger cuentas activas.
        throw new NullPointerException("User roles must not be null");
      }
    } catch (NoSuchElementException e) {
      throw new RoleRetrievalException("Error retrieving roles", e);
    } catch (Exception e) {
      throw new RoleRetrievalException("Unexpected error occurred", e);
    }
    return roles;
  }
}
