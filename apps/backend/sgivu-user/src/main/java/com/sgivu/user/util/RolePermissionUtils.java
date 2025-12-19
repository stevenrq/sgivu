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

/**
 * Utilidades para normalizar roles y permisos recibidos desde el API.
 *
 * <p>Permite convertir los nombres enviados por el cliente en entidades persistidas evitando
 * duplicar lógica de seguridad en controladores/servicios. También garantiza un rol base cuando no
 * se envían authorities.
 */
public class RolePermissionUtils {
  private static final String USER = "USER";

  private RolePermissionUtils() {}

  /**
   * Obtiene los nombres de los roles y permisos a partir de un conjunto de roles.
   *
   * @param roles El conjunto de roles.
   * @return Un conjunto de cadenas con los nombres de los roles y permisos.
   */
  public static Set<String> getRolesAndPermissions(Set<Role> roles) {
    Objects.requireNonNull(roles, "El conjunto de roles no debe ser nulo");

    return Stream.concat(
            roles.stream().map(Role::getName),
            roles.stream().flatMap(role -> role.getPermissions().stream()).map(Permission::getName))
        .collect(Collectors.toSet());
  }

  /**
   * Obtiene los roles a partir de un objeto que puede ser una instancia de User o
   * UserUpdateRequest.
   *
   * @param user El objeto del cual se obtendrán los roles.
   * @param roleRepository El repositorio de roles para buscar los roles por nombre.
   * @return Un conjunto de roles asociados al usuario o solicitud de actualización.
   */
  public static Set<Role> getRoles(Object user, RoleRepository roleRepository) {
    Set<Role> roles = new HashSet<>();

    if (user instanceof User u) {
      roles = getRolesFromUser(u, roleRepository);
    } else if (user instanceof UserUpdateRequest ur) {
      roles = getRolesFromUserUpdateRequest(ur, roleRepository);
    }
    return roles;
  }

  /**
   * Obtiene los roles a partir de un objeto User.
   *
   * @param user El objeto User del cual se obtendrán los roles.
   * @param roleRepository El repositorio de roles para buscar los roles por nombre.
   * @return Un conjunto de roles asociados al usuario.
   */
  private static Set<Role> getRolesFromUser(User user, RoleRepository roleRepository) {
    Set<Role> roles = new HashSet<>();

    try {
      Set<String> rolesAndPermissionsOfUser = user.getRolesAndPermissions();

      if (rolesAndPermissionsOfUser.isEmpty()) {
        // Rol por defecto para habilitar navegación básica del usuario en SGIVU.
        roles.add(roleRepository.findByName(USER).orElseThrow());
      } else {
        for (String role : rolesAndPermissionsOfUser) {
          Optional<Role> roleOptional = roleRepository.findByName(role);
          roleOptional.ifPresent(roles::add);
        }
      }
    } catch (NoSuchElementException e) {
      throw new RoleRetrievalException("Error al recuperar roles", e);
    } catch (IllegalArgumentException e) {
      throw new RoleRetrievalException("Nombre de rol no válido", e);
    } catch (Exception e) {
      throw new RoleRetrievalException("Se produjo un error inesperado", e);
    }
    return roles;
  }

  /**
   * Obtiene los roles a partir de un objeto UserUpdateRequest.
   *
   * @param userUpdateRequest El objeto UserUpdateRequest del cual se obtendrán los roles.
   * @param roleRepository El repositorio de roles para buscar los roles por nombre.
   * @return Un conjunto de roles asociados a la solicitud de actualización.
   */
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
        throw new NullPointerException("Los roles de usuario no deben ser nulos");
      }
    } catch (NoSuchElementException e) {
      throw new RoleRetrievalException("Error al recuperar roles", e);
    } catch (Exception e) {
      throw new RoleRetrievalException("Se produjo un error inesperado", e);
    }
    return roles;
  }
}
