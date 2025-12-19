package com.sgivu.client.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.client.dto.PersonSearchCriteria;
import com.sgivu.client.entity.Address;
import com.sgivu.client.entity.Person;
import com.sgivu.client.repository.PersonRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PersonServiceImplTest {

  @Mock private PersonRepository personRepository;

  private PersonServiceImpl personService;

  @BeforeEach
  void setUp() {
    personService = new PersonServiceImpl(personRepository);
  }

  @Test
  void updateWhenPersonExistsUpdatesNamesOnly() {
    Address existingAddress = new Address();
    Person existing = samplePerson();
    existing.setId(1L);
    existing.setEmail("old@email.com");
    existing.setPhoneNumber(12345L);
    existing.setAddress(existingAddress);
    existing.setFirstName("Old");
    existing.setLastName("Name");

    Person incoming = new Person();
    incoming.setFirstName("New");
    incoming.setLastName("Person");
    incoming.setEmail("new@email.com");
    incoming.setPhoneNumber(99999L);
    incoming.setAddress(new Address());

    when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(personRepository.save(any(Person.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Person> result = personService.update(1L, incoming);

    assertThat(result).isPresent();
    Person saved = result.get();
    assertThat(saved.getFirstName()).isEqualTo("New");
    assertThat(saved.getLastName()).isEqualTo("Person");
    assertThat(saved.getEmail()).isEqualTo("old@email.com");
    assertThat(saved.getPhoneNumber()).isEqualTo(12345L);
    assertThat(saved.getAddress()).isSameAs(existingAddress);
    verify(personRepository).save(existing);
  }

  @Test
  void updateWhenPersonNotFoundReturnsEmpty() {
    Person incoming = samplePerson();
    when(personRepository.findById(2L)).thenReturn(Optional.empty());

    Optional<Person> result = personService.update(2L, incoming);

    assertThat(result).isEmpty();
    verify(personRepository, never()).save(any(Person.class));
  }

  @Test
  void changeStatusUpdatesFlagWhenPersonExists() {
    Person existing = samplePerson();
    existing.setId(3L);
    existing.setEnabled(false);
    when(personRepository.findById(3L)).thenReturn(Optional.of(existing));
    when(personRepository.save(any(Person.class))).thenReturn(existing);

    boolean updated = personService.changeStatus(3L, true);

    assertThat(updated).isTrue();
    assertThat(existing.isEnabled()).isTrue();
    verify(personRepository).save(existing);
  }

  @Test
  void changeStatusReturnsFalseWhenPersonMissing() {
    when(personRepository.findById(4L)).thenReturn(Optional.empty());

    boolean updated = personService.changeStatus(4L, false);

    assertThat(updated).isFalse();
    verify(personRepository, never()).save(any(Person.class));
  }

  @Test
  void searchWithoutPaginationUsesUnpagedSpecification() {
    PersonSearchCriteria criteria =
        PersonSearchCriteria.builder().name("ana").city("quito").build();
    List<Person> persons = List.of(samplePerson());
    when(personRepository.findAll(
            ArgumentMatchers.<Specification<Person>>any(), eq(Pageable.unpaged())))
        .thenReturn(new PageImpl<>(persons));

    List<Person> result = personService.search(criteria);

    assertThat(result).containsExactlyElementsOf(persons);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(personRepository)
        .findAll(ArgumentMatchers.<Specification<Person>>any(), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue()).isEqualTo(Pageable.unpaged());
  }

  @Test
  void searchWithPaginationDelegatesToRepository() {
    PersonSearchCriteria criteria = PersonSearchCriteria.builder().email("mail@test.com").build();
    Pageable pageable = PageRequest.of(1, 10);
    when(personRepository.findAll(ArgumentMatchers.<Specification<Person>>any(), eq(pageable)))
        .thenReturn(Page.empty(pageable));

    Page<Person> page = personService.search(criteria, pageable);

    assertThat(page.getContent()).isEmpty();
    verify(personRepository).findAll(ArgumentMatchers.<Specification<Person>>any(), eq(pageable));
  }

  @Test
  void findByNationalIdDelegatesToRepository() {
    Person person = samplePerson();
    when(personRepository.findByNationalId(9876L)).thenReturn(Optional.of(person));

    Optional<Person> result = personService.findByNationalId(9876L);

    assertThat(result).contains(person);
  }

  @Test
  void findByFirstNameOrLastNameDelegatesToRepository() {
    List<Person> persons = List.of(samplePerson());
    when(personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            "ana", "ana"))
        .thenReturn(persons);

    List<Person> result = personService.findByFirstNameOrLastName("ana");

    assertThat(result).containsExactlyElementsOf(persons);
  }

  private Person samplePerson() {
    Person person = new Person();
    person.setNationalId(123456789L);
    person.setFirstName("Jane");
    person.setLastName("Doe");
    person.setEmail("jane.doe@test.com");
    person.setPhoneNumber(5551234L);
    person.setAddress(new Address());
    return person;
  }
}
