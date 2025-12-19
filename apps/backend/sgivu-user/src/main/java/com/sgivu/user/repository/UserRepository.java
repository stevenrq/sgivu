package com.sgivu.user.repository;

import com.sgivu.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio JPA para usuarios.
 *
 * <p>Incluye consultas derivadas usadas por el Authorization Server (búsqueda por username) y por
 * paneles operativos (conteo de habilitados, búsqueda por nombre).
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

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
   * @param isEnabled indicador de estado deseado.
   * @return cantidad de usuarios que cumplen la condición.
   */
  long countByIsEnabled(boolean isEnabled);

  /**
   * Busca usuarios cuyos nombres o apellidos contengan el fragmento indicado.
   *
   * @param firstName fragmento a comparar contra el nombre.
   * @param lastName fragmento a comparar contra el apellido.
   * @return lista de coincidencias parciales, ignorando mayúsculas/minúsculas.
   */
  List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
      String firstName, String lastName);
}
