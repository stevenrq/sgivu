package com.sgivu.user.repository;

import com.sgivu.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends PersonRepository<User>, JpaSpecificationExecutor<User> {

  Optional<User> findByUsername(String username);

  long countByEnabled(boolean enabled);
}
