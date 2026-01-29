package com.sgivu.client.service;

import com.sgivu.client.dto.PersonSearchCriteria;
import com.sgivu.client.entity.Person;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PersonService extends ClientService<Person> {

  Optional<Person> findByNationalId(Long nationalId);

  List<Person> findByFirstNameOrLastName(String name);

  List<Person> search(PersonSearchCriteria criteria);

  Page<Person> search(PersonSearchCriteria criteria, Pageable pageable);
}
