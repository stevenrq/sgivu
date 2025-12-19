package com.sgivu.client.repository;

import com.sgivu.client.entity.Person;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de personas con consultas específicas para onboarding y validación en contratos.
 */
public interface PersonRepository extends ClientRepository<Person> {

  /**
   * Busca persona por documento nacional para detectar duplicados.
   *
   * @param nationalId documento
   * @return persona si existe
   */
  Optional<Person> findByNationalId(Long nationalId);

  /**
   * Búsqueda parcial por nombre o apellido para autocompletar en frontales de venta.
   *
   * @param firstName nombre
   * @param lastName apellido
   * @return coincidencias
   */
  List<Person> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
      String firstName, String lastName);
}
