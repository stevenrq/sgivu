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
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class PersonController implements PersonApi {

  private final PersonService personService;
  private final ClientMapper clientMapper;

  public PersonController(PersonService personService, ClientMapper clientMapper) {
    this.personService = personService;
    this.clientMapper = clientMapper;
  }

  @Override
  @PreAuthorize("hasAuthority('person:create')")
  public ResponseEntity<PersonResponse> create(Person person, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    Person savedPerson = personService.save(person);
    PersonResponse response = clientMapper.toPersonResponse(savedPerson);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<PersonResponse> getById(Long id) {
    return personService
        .findById(id)
        .map(person -> ResponseEntity.ok(clientMapper.toPersonResponse(person)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<List<PersonResponse>> getAll() {
    List<PersonResponse> responses =
        personService.findAll().stream().map(clientMapper::toPersonResponse).toList();
    return ResponseEntity.ok(responses);
  }

  @Override
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<Page<PersonResponse>> getAllPaginated(Integer page) {
    Page<Person> personPage = personService.findAll(PageRequest.of(page, 10));
    Page<PersonResponse> responsePage = personPage.map(clientMapper::toPersonResponse);
    return ResponseEntity.ok(responsePage);
  }

  @Override
  @PreAuthorize("hasAuthority('person:update')")
  public ResponseEntity<PersonResponse> update(
      Long id, Person person, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().build();
    }
    return personService
        .update(id, person)
        .map(personUpdated -> ResponseEntity.ok(clientMapper.toPersonResponse(personUpdated)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('person:delete')")
  public ResponseEntity<Void> deleteById(Long id) {
    Optional<Person> personOptional = personService.findById(id);

    if (personOptional.isPresent()) {
      personService.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }

  @Override
  @PreAuthorize("hasAuthority('person:update')")
  public ResponseEntity<Map<String, Boolean>> changeStatus(Long id, boolean enabled) {
    boolean isUpdated = personService.changeStatus(id, enabled);
    if (isUpdated) {
      return ResponseEntity.ok(Collections.singletonMap("status", enabled));
    }
    return ResponseEntity.notFound().build();
  }

  @Override
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

  @Override
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<List<PersonResponse>> searchPersons(
      String name, String email, Long nationalId, Long phoneNumber, Boolean enabled, String city) {

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

  @Override
  @PreAuthorize("hasAuthority('person:read')")
  public ResponseEntity<Page<PersonResponse>> searchPersonsPaginated(
      Integer page,
      Integer size,
      String name,
      String email,
      Long nationalId,
      Long phoneNumber,
      Boolean enabled,
      String city) {

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

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
