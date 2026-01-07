package com.sgivu.user.service;

import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Orquesta la lógica de negocio para gestionar usuarios del ecosistema SGIVU.
 *
 * <p>Centraliza validaciones, búsqueda dinámica y administración de estados para que los
 * controladores expongan flujos consistentes con los contratos del Authorization Server y el
 * inventario de roles/permisos compartido entre microservicios.
 */
public interface UserService extends PersonService<User> {

  /**
   * Consulta por username, usado por el Authorization Server para construir el principal.
   *
   * @param username alias único.
   * @return usuario si existe, de lo contrario vacío.
   */
  Optional<User> findByUsername(String username);

  /**
   * Actualiza datos específicos de un usuario.
   *
   * @param id identificador
   * @param userUpdateRequest datos entrantes
   * @return usuario actualizado si existe
   */
  Optional<User> update(Long id, UserUpdateRequest userUpdateRequest);

  void deleteById(Long id);

  /**
   * Cambia el estado de un usuario para habilitar/bloquear acceso al resto de microservicios.
   *
   * @param id identificador del usuario.
   * @param isEnabled nuevo estado objetivo.
   * @return {@code true} si el estado se cambió, {@code false} si el usuario no existe.
   */
  boolean changeStatus(Long id, boolean isEnabled);

  /**
   * @return cantidad de usuarios activos (habilitados) en el sistema.
   */
  long countActiveUsers();

  /**
   * Búsqueda multi-criterio sin paginar.
   *
   * @param criteria filtros combinados (AND) para inventario de usuarios.
   * @return lista de coincidencias.
   */
  List<User> search(UserFilterCriteria criteria);

  /**
   * Búsqueda multi-criterio paginada.
   *
   * @param criteria filtros combinados.
   * @param pageable configuración de página.
   * @return página de resultados.
   */
  Page<User> search(UserFilterCriteria criteria, Pageable pageable);
}
