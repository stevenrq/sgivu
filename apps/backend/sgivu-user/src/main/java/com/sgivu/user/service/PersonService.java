package com.sgivu.user.service;

import com.sgivu.user.entity.Person;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz de servicio para la gestión de personas. Proporciona operaciones CRUD y búsqueda para
 * entidades que extiendan de {@link Person}.
 *
 * @param <T> tipo genérico que extiende de {@link Person}
 */
public interface PersonService<T extends Person> {

  /**
   * Guarda una nueva persona en la base de datos.
   *
   * @param user la persona a guardar
   * @return la persona guardada con su identificador asignado
   */
  T save(T user);

  /**
   * Busca una persona por su identificador.
   *
   * @param id el identificador único de la persona
   * @return un {@link Optional} que contiene la persona si existe, o vacío si no se encuentra
   */
  Optional<T> findById(Long id);

  /**
   * Obtiene todas las personas almacenadas en la base de datos.
   *
   * @return una lista con todas las personas
   */
  List<T> findAll();

  /**
   * Obtiene todas las personas con paginación.
   *
   * @param pageable información de paginación (número de página, tamaño, ordenamiento)
   * @return una página con las personas solicitadas
   */
  Page<T> findAll(Pageable pageable);

  /**
   * Actualiza los datos de una persona existente.
   *
   * @param id el identificador de la persona a actualizar
   * @param person la persona con los nuevos datos
   * @return un {@link Optional} que contiene la persona actualizada si existe, o vacío si la
   *     persona no se encuentra
   */
  Optional<T> update(Long id, T person);

  /**
   * Elimina una persona por su identificador.
   *
   * @param id el identificador de la persona a eliminar
   */
  void deleteById(Long id);

  /**
   * Busca personas por nombre (nombre de pila o apellido).
   *
   * @param name el nombre a buscar (puede coincidir parcialmente con el nombre o apellido)
   * @return una lista de personas que coincidan con el criterio de búsqueda
   */
  List<T> findByFirstNameOrLastName(String name);
}
