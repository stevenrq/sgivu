package com.sgivu.user.repository;

import com.sgivu.user.entity.Person;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository<T extends Person> extends JpaRepository<T, Long> {

  List<T> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
      String firstName, String lastName);
}
