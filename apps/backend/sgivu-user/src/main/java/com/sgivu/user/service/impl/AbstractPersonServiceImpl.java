package com.sgivu.user.service.impl;

import com.sgivu.user.entity.Person;
import com.sgivu.user.repository.PersonRepository;
import com.sgivu.user.service.PersonService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public abstract class AbstractPersonServiceImpl<T extends Person, R extends PersonRepository<T>>
    implements PersonService<T> {

  protected final R personRepository;

  protected AbstractPersonServiceImpl(R personRepository) {
    this.personRepository = personRepository;
  }

  @Transactional
  @Override
  public T save(T user) {
    return personRepository.save(user);
  }

  @Override
  public Optional<T> findById(Long id) {
    return personRepository.findById(id);
  }

  @Override
  public List<T> findAll() {
    return personRepository.findAll();
  }

  @Override
  public Page<T> findAll(Pageable pageable) {
    return personRepository.findAll(pageable);
  }

  @Transactional
  @Override
  public Optional<T> update(Long id, T person) {
    return personRepository
        .findById(id)
        .map(
            existing -> {
              existing.setFirstName(person.getFirstName());
              existing.setLastName(person.getLastName());
              existing.setAddress(person.getAddress());
              existing.setPhoneNumber(person.getPhoneNumber());
              existing.setEmail(person.getEmail());
              return personRepository.save(existing);
            });
  }

  @Transactional
  @Override
  public void deleteById(Long id) {
    personRepository.deleteById(id);
  }

  @Override
  public List<T> findByFirstNameOrLastName(String name) {
    return personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        name, name);
  }
}
