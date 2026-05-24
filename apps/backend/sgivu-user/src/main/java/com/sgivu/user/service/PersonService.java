package com.sgivu.user.service;

import com.sgivu.user.entity.Person;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonService<T extends Person> {

  T save(T user);

  Optional<T> findById(Long id);

  List<T> findAll();

  List<T> findAllByIds(Iterable<Long> ids);

  Page<T> findAll(Pageable pageable);

  Optional<T> update(Long id, T person);

  void deleteById(Long id);

  long count();

  List<T> findByFirstNameOrLastName(String name);
}
