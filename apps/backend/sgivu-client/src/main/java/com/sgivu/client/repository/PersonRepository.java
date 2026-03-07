package com.sgivu.client.repository;

import com.sgivu.client.entity.Person;
import java.util.List;
import java.util.Optional;

public interface PersonRepository extends ClientRepository<Person> {

  Optional<Person> findByNationalId(Long nationalId);

  List<Person> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
      String firstName, String lastName);
}
