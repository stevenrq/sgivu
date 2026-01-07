package com.sgivu.user.repository;

import com.sgivu.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio JPA para usuarios.
 *
 * <p>Incluye consultas derivadas usadas por el Authorization Server (búsqueda por username) y por
 * paneles operativos (conteo de habilitados, búsqueda por nombre).
 */
public interface UserRepository extends PersonRepository<User>, JpaSpecificationExecutor<User> {

  /**
   * Busca un usuario por su {@code username}, utilizado durante la autenticación para construir el
   * principal y sus autoridades.
   *
   * @param username alias único del usuario.
   * @return coincidencia exacta o {@link Optional#empty()} si no existe.
   */
  Optional<User> findByUsername(String username);

  /**
   * Cuenta usuarios según su estado de habilitación.
   *
   * @param enabled indicador de estado deseado.
   * @return cantidad de usuarios que cumplen la condición.
   */
  long countByEnabled(boolean enabled);
}
