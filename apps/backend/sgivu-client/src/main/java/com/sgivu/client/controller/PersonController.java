package com.sgivu.client.controller;

import com.sgivu.client.controller.api.PersonApi;
import com.sgivu.client.dto.PersonResponse;
import com.sgivu.client.dto.PersonSearchCriteria;
import com.sgivu.client.entity.Person;
import com.sgivu.client.mapper.ClientMapper;
import com.sgivu.client.service.PersonService;
import java.util.*;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * API REST para gestionar personas dentro del catálogo de clientes SGIVU. Expone operaciones
 * consumidas por inventario, predicción de demanda y contratos para validar datos de compradores y
 * vendedores.
 *
 * <p>Los endpoints requieren autorización granular via JWT (rolesAndPermissions) y permiten
 * invocaciones internas desde servicios de SGIVU usando la clave compartida.
 */
@RefreshScope
@RestController
public class PersonController implements PersonApi {

  private final PersonService personService;
  private final ClientMapper clientMapper;

  public PersonController(PersonService personService, ClientMapper clientMapper) {
    this.personService = personService;
    this.clientMapper = clientMapper;
  }

  /**
   * Registra un nuevo cliente persona que podrá ser utilizado en flujos de compra y venta de
   * vehículos usados.
   *
   * @param person datos de la persona incluyendo información de contacto y domicilio
   * @param bindingResult resultado de validaciones básicas del request
   * @return {@link PersonResponse} con la persona persistida
   *     <p>Valida el payload antes de delegar a la capa de servicio; la creación queda centralizada
   *     para mantener coherencia de clientes en todo el ecosistema.
   */
  @PostMapping
  @PreAuthorize("hasAuthority('person:create')")
  public ResponseEntity<PersonResponse> create(
      @RequestBody Person person, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    Person savedPerson = personService.save(person);
    PersonResponse response = clientMapper.toPersonResponse(savedPerson);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Recupera una persona por su identificador técnico para enriquecer operaciones como contratos o
   * auditorías de inventario.
   *
   * @param id identificador de la persona
   * @return {@link PersonResponse} si existe; 404 en caso contrario
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<PersonResponse> getById(@PathVariable Long id) {
    return personService
        .findById(id)
        .map(person -> ResponseEntity.ok(clientMapper.toPersonResponse(person)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Devuelve la lista completa de personas habilitadas o no, usada por backoffice para campañas de
   * retención y análisis de demanda.
   *
   * @return lista de {@link PersonResponse}
   */
  @GetMapping
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<List<PersonResponse>> getAll() {
    List<PersonResponse> responses =
        personService.findAll().stream().map(clientMapper::toPersonResponse).toList();
    return ResponseEntity.ok(responses);
  }

  /**
   * Obtiene personas en páginas de tamaño fijo para integraciones que consumen lotes (por ejemplo,
   * sincronización de contratos).
   *
   * @param page número de página solicitada
   * @return página de {@link PersonResponse}
   */
  @GetMapping("/page/{page}")
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<Page<PersonResponse>> getAllPaginated(@PathVariable Integer page) {
    Page<Person> personPage = personService.findAll(PageRequest.of(page, 10));
    Page<PersonResponse> responsePage = personPage.map(clientMapper::toPersonResponse);
    return ResponseEntity.ok(responsePage);
  }

  /**
   * Actualiza datos básicos de contacto de una persona preservando consistencia con otros
   * microservicios que referencian el cliente.
   *
   * @param id identificador de la persona a actualizar
   * @param person datos nuevos
   * @param bindingResult validaciones de entrada
   * @return {@link PersonResponse} actualizado o 404 si no existe
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('person:update')")
  public ResponseEntity<PersonResponse> update(
      @PathVariable Long id, @RequestBody Person person, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    return personService
        .update(id, person)
        .map(personUpdated -> ResponseEntity.ok(clientMapper.toPersonResponse(personUpdated)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Elimina lógicamente un cliente persona utilizado en flujos de ventas cuando ya no debe operar
   * en el ecosistema.
   *
   * @param id identificador de la persona
   * @return 204 si se elimina, 404 si no existe
   *     <p>La eliminación delega en la capa de servicio; si otras tablas referencian el cliente, la
   *     lógica de dominio debería moverlo a un estado inactivo en lugar de removerlo.
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('person:delete')")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    Optional<Person> personOptional = personService.findById(id);

    if (personOptional.isPresent()) {
      personService.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Habilita o deshabilita a una persona para nuevos contratos o pedidos de inventario según reglas
   * de riesgo y auditoría.
   *
   * @param id identificador de la persona
   * @param enabled bandera deseada
   * @return mapa con el estado aplicado o 404 si no existe
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('person:update')")
  public ResponseEntity<Map<String, Boolean>> changeStatus(
      @PathVariable Long id, @RequestBody boolean enabled) {
    boolean isUpdated = personService.changeStatus(id, enabled);
    if (isUpdated) {
      return ResponseEntity.ok(Collections.singletonMap("status", enabled));
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Calcula métricas básicas de personas activas/inactivas para alimentar tableros de ventas y
   * demanda.
   *
   * @return mapa con totales segmentados
   */
  @GetMapping("/count")
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<Map<String, Long>> getPersonCounts() {
    long totalPersons = personService.findAll().size();
    long activePersons = personService.countByEnabled(true);
    long inactivePersons = totalPersons - activePersons;

    Map<String, Long> counts = new HashMap<>(Map.of("totalPersons", totalPersons));
    counts.put("activePersons", activePersons);
    counts.put("inactivePersons", inactivePersons);

    return ResponseEntity.ok(counts);
  }

  /**
   * Búsqueda flexible de personas para validar clientes durante la creación de contratos o reservas
   * de vehículos usados.
   *
   * @param name nombre o apellido a buscar (like)
   * @param email correo de contacto
   * @param nationalId documento nacional
   * @param phoneNumber teléfono registrado
   * @param enabled estado de habilitación
   * @param city ciudad asociada al domicilio
   * @return lista filtrada de {@link PersonResponse}
   * @see PersonSpecifications#withFilters(PersonSearchCriteria)
   */
  @GetMapping("/search")
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<List<PersonResponse>> searchPersons(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long nationalId,
      @RequestParam(required = false) Long phoneNumber,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String city) {

    PersonSearchCriteria criteria =
        PersonSearchCriteria.builder()
            .name(trimToNull(name))
            .email(trimToNull(email))
            .nationalId(nationalId)
            .phoneNumber(phoneNumber)
            .enabled(enabled)
            .city(trimToNull(city))
            .build();

    List<PersonResponse> personResponses =
        personService.search(criteria).stream().map(clientMapper::toPersonResponse).toList();
    return ResponseEntity.ok(personResponses);
  }

  /**
   * Variante paginada de búsqueda para integraciones que consumen resultados por lote (por ejemplo,
   * conciliación de cartera).
   *
   * @param page página solicitada
   * @param size tamaño de página
   * @param name nombre o apellido
   * @param email correo electrónico
   * @param nationalId documento
   * @param phoneNumber teléfono
   * @param enabled estado
   * @param city ciudad
   * @return página de {@link PersonResponse} que cumplen los filtros
   * @see PersonSpecifications#withFilters(PersonSearchCriteria)
   */
  @GetMapping("/search/page/{page}")
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<Page<PersonResponse>> searchPersonsPaginated(
      @PathVariable Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long nationalId,
      @RequestParam(required = false) Long phoneNumber,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String city) {

    PersonSearchCriteria criteria =
        PersonSearchCriteria.builder()
            .name(trimToNull(name))
            .email(trimToNull(email))
            .nationalId(nationalId)
            .phoneNumber(phoneNumber)
            .enabled(enabled)
            .city(trimToNull(city))
            .build();

    Page<PersonResponse> responsePage =
        personService
            .search(criteria, PageRequest.of(page, size))
            .map(clientMapper::toPersonResponse);
    return ResponseEntity.ok(responsePage);
  }

  /** Normaliza texto para evitar filtros con espacios únicamente. */
  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
