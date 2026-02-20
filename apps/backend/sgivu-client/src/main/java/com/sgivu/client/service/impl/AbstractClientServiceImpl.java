package com.sgivu.client.service.impl;

import com.sgivu.client.entity.Client;
import com.sgivu.client.repository.ClientRepository;
import com.sgivu.client.service.ClientService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public abstract class AbstractClientServiceImpl<T extends Client, R extends ClientRepository<T>>
    implements ClientService<T> {

  protected final R clientRepository;

  protected AbstractClientServiceImpl(R clientRepository) {
    this.clientRepository = clientRepository;
  }

  @Transactional
  @Override
  public T save(T client) {
    return clientRepository.save(client);
  }

  @Override
  public List<T> findAll() {
    return clientRepository.findAll();
  }

  @Override
  public Page<T> findAll(Pageable pageable) {
    return clientRepository.findAll(pageable);
  }

  @Override
  public Optional<T> findById(Long id) {
    return clientRepository.findById(id);
  }

  @Transactional
  @Override
  public Optional<T> update(Long id, T client) {
    return clientRepository
        .findById(id)
        .map(
            existing -> {
              existing.setEmail(client.getEmail());
              existing.setPhoneNumber(client.getPhoneNumber());
              existing.setAddress(client.getAddress());
              return clientRepository.save(existing);
            });
  }

  @Transactional
  @Override
  public void deleteById(Long id) {
    clientRepository.deleteById(id);
  }

  @Override
  public Optional<T> findByEmail(String email) {
    return clientRepository.findByEmail(email);
  }

  @Transactional
  @Override
  public boolean changeStatus(Long id, boolean enabled) {
    return clientRepository
        .findById(id)
        .map(
            client -> {
              client.setEnabled(enabled);
              clientRepository.save(client);
              return true;
            })
        .orElse(false);
  }

  @Override
  public long countByEnabled(boolean enabled) {
    return clientRepository.countByEnabled(enabled);
  }
}
