package com.sgivu.user.service;

import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService extends PersonService<User> {

  Optional<User> findByUsername(String username);

  Optional<User> update(Long id, UserUpdateRequest userUpdateRequest);

  void deleteById(Long id);

  boolean changeStatus(Long id, boolean enabled);

  long countActiveUsers();

  List<User> search(UserFilterCriteria criteria);

  Page<User> search(UserFilterCriteria criteria, Pageable pageable);
}
