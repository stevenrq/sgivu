package com.sgivu.client.repository;

import com.sgivu.client.entity.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientRepository<T extends Client>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

  Optional<T> findByEmail(String email);

  long countByEnabled(boolean enabled);
}
