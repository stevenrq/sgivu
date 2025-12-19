package com.sgivu.user.repository;

import com.sgivu.user.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio base para entidades que heredan de {@link Person}. */
public interface PersonRepository extends JpaRepository<Person, Long> {}
