package com.sgivu.client.service;

import com.sgivu.client.entity.Client;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService<T extends Client> {

  T save(T client);

  Optional<T> findById(Long id);

  List<T> findAll();

  Page<T> findAll(Pageable pageable);

  Optional<T> update(Long id, T client);

  void deleteById(Long id);

  Optional<T> findByEmail(String email);

  boolean changeStatus(Long id, boolean enabled);

  long countByEnabled(boolean enabled);
}
