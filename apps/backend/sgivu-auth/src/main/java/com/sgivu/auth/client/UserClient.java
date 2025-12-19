package com.sgivu.auth.client;

import com.sgivu.auth.dto.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Cliente HTTP hacia el microservicio {@code sgivu-user}.
 *
 * <p>se usa durante el login para recuperar credenciales, roles y permisos de usuarios que
 * gestionan inventario, contratos y ventas.
 */
@HttpExchange("/v1/users")
public interface UserClient {

  /**
   * Consulta un usuario por username en el servicio de usuarios.
   *
   * @param username nombre único del usuario.
   * @return DTO de usuario con roles/permisos.
   */
  @GetExchange("/username/{username}")
  User findByUsername(@PathVariable String username);
}
