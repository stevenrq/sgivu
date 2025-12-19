package com.sgivu.client.service;

import com.sgivu.client.dto.PersonSearchCriteria;
import com.sgivu.client.entity.Person;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Servicio de dominio para clientes persona dentro de SGIVU.
 * @see com.sgivu.client.service.impl.PersonServiceImpl
 */
public interface PersonService extends ClientService<Person> {

  /**
   * Localiza una persona por documento nacional para validación previa de contratos.
   *
   * @param nationalId documento
   * @return persona encontrada o vacía
   */
  Optional<Person> findByNationalId(Long nationalId);

  /**
   * Búsqueda parcial por nombre o apellido para experiencias de autocompletado.
   *
   * @param name fragmento de nombre/apellido
   * @return coincidencias
   */
  List<Person> findByFirstNameOrLastName(String name);

  /**
   * Búsqueda flexible sin paginar combinando criterios de contacto y estado.
   *
   * @param criteria filtros
   * @return lista de personas
   */
  List<Person> search(PersonSearchCriteria criteria);

  /**
   * Búsqueda paginada usando especificaciones JPA.
   *
   * @param criteria filtros
   * @param pageable configuración de paginación
   * @return página de personas
   */
  Page<Person> search(PersonSearchCriteria criteria, Pageable pageable);
}
