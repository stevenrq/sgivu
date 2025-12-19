package com.sgivu.client.service.impl;

import com.sgivu.client.dto.PersonSearchCriteria;
import com.sgivu.client.entity.Person;
import com.sgivu.client.repository.PersonRepository;
import com.sgivu.client.service.PersonService;
import com.sgivu.client.specification.PersonSpecifications;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PersonServiceImpl extends AbstractClientServiceImpl<Person, PersonRepository>
    implements PersonService {

  private final PersonRepository personRepository;

  public PersonServiceImpl(PersonRepository personRepository) {
    super(personRepository);
    this.personRepository = personRepository;
  }

  @Override
  public Optional<Person> findByNationalId(Long nationalId) {
    return personRepository.findByNationalId(nationalId);
  }

  @Override
  public List<Person> findByFirstNameOrLastName(String name) {
    return personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        name, name);
  }

  @Override
  public List<Person> search(PersonSearchCriteria criteria) {
    return search(criteria, Pageable.unpaged()).getContent();
  }

  @Override
  public Page<Person> search(PersonSearchCriteria criteria, Pageable pageable) {
    return personRepository.findAll(PersonSpecifications.withFilters(criteria), pageable);
  }

  @Override
  public Optional<Person> update(Long id, Person client) {
    return personRepository
        .findById(id)
        .map(
            existing -> {
              existing.setFirstName(client.getFirstName());
              existing.setLastName(client.getLastName());
              return personRepository.save(existing);
            });
  }
}
