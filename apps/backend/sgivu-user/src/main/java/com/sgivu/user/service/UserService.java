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
public interface UserService {

  /**
   * Persiste un nuevo usuario aplicando las reglas de roles y codificación de credenciales.
   *
   * @param user entidad proveniente de la API, incluyendo roles solicitados.
   * @return usuario almacenado con roles normalizados y password cifrada.
   */
  User save(User user);

  /**
   * Recupera un usuario por identificador para escenarios de autoedición y administración.
   *
   * @param id identificador único.
   * @return usuario si existe, de lo contrario vacío.
   */
  Optional<User> findById(Long id);

  /**
   * Consulta por username, usado por el Authorization Server para construir el principal.
   *
   * @param username alias único.
   * @return usuario si existe, de lo contrario vacío.
   */
  Optional<User> findByUsername(String username);

  /** @return todos los usuarios activos e inactivos (uso administrativo). */
  List<User> findAll();

  /**
   * Variante paginada para consumo en paneles y listados.
   *
   * @param pageable configuraciones de página/orden.
   * @return página de usuarios.
   */
  Page<User> findAll(Pageable pageable);

  /**
   * Actualiza datos de un usuario respetando validaciones y roles solicitados.
   *
   * @param id identificador destino.
   * @param userUpdateRequest DTO con cambios permitidos.
   * @return usuario actualizado o vacío si no existe.
   */
  Optional<User> update(Long id, UserUpdateRequest userUpdateRequest);

  /**
   * Elimina un usuario de forma permanente (suele usarse para cuentas de prueba o limpieza).
   *
   * @param id identificador destino.
   */
  void deleteById(Long id);

  /**
   * Cambia el estado de un usuario para habilitar/bloquear acceso al resto de microservicios.
   *
   * @param id identificador del usuario.
   * @param isEnabled nuevo estado objetivo.
   * @return {@code true} si el estado se cambió, {@code false} si el usuario no existe.
   */
  boolean changeStatus(Long id, boolean isEnabled);

  /** @return cantidad de usuarios activos (habilitados) en el sistema. */
  long countActiveUsers();

  /**
   * Búsqueda liviana para autocompletados por nombre o apellido.
   *
   * @param name fragmento a buscar.
   * @return coincidencias parciales.
   */
  List<User> findByFirstNameOrLastName(String name);

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
