package com.sgivu.user.service.impl;

import static com.sgivu.user.util.RolePermissionUtils.getRoles;

import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.User;
import com.sgivu.user.repository.RoleRepository;
import com.sgivu.user.repository.UserRepository;
import com.sgivu.user.service.UserService;
import com.sgivu.user.specification.UserSpecifications;
import com.sgivu.user.util.RolePermissionUtils;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl extends AbstractPersonServiceImpl<User, UserRepository>
    implements UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    super(userRepository);
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public User save(User user) {
    user.setRoles(getRoles(user, roleRepository));
    if (user.getPassword() == null || user.getPassword().isEmpty()) {
      throw new NullPointerException("La contraseña no puede ser nula o vacía");
    }
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    return userRepository.save(user);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<User> findAll(Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  @Transactional
  public Optional<User> update(Long id, UserUpdateRequest userUpdateRequest) {
    Optional<User> userOptional = userRepository.findById(id);

    if (userOptional.isPresent()) {
      User userToUpdate = userOptional.orElseThrow();

      userToUpdate.setFirstName(userUpdateRequest.getFirstName());
      userToUpdate.setLastName(userUpdateRequest.getLastName());
      userToUpdate.setAddress(userUpdateRequest.getAddress());
      userToUpdate.setPhoneNumber(userUpdateRequest.getPhoneNumber());
      userToUpdate.setEmail(userUpdateRequest.getEmail());
      userToUpdate.setUsername(userUpdateRequest.getUsername());

      userToUpdate.setRoles(RolePermissionUtils.getRoles(userToUpdate, roleRepository));
      if (userUpdateRequest.getPassword() != null && !userUpdateRequest.getPassword().isEmpty()) {
        userToUpdate.setPassword(passwordEncoder.encode(userUpdateRequest.getPassword()));
      }

      return Optional.of(userRepository.save(userToUpdate));
    }
    return Optional.empty();
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    userRepository.deleteById(id);
  }

  @Override
  @Transactional
  public boolean changeStatus(Long id, boolean enabled) {
    Optional<User> userOptional = userRepository.findById(id);

    if (userOptional.isPresent()) {
      User user = userOptional.orElseThrow();

      user.setEnabled(enabled);
      userRepository.save(user);
      return true;
    }
    return false;
  }

  @Override
  public long countActiveUsers() {
    return userRepository.countByEnabled(true);
  }

  @Override
  public List<User> findByFirstNameOrLastName(String name) {
    return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        name, name);
  }

  @Override
  public List<User> search(UserFilterCriteria criteria) {
    return search(criteria, Pageable.unpaged()).getContent();
  }

  @Transactional(readOnly = true)
  public Page<User> search(UserFilterCriteria criteria, Pageable pageable) {
    return userRepository.findAll(UserSpecifications.withFilters(criteria), pageable);
  }
}
