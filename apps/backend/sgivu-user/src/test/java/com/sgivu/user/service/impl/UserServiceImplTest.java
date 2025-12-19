package com.sgivu.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sgivu.user.ServiceTestDataProvider;
import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.Role;
import com.sgivu.user.entity.User;
import com.sgivu.user.repository.RoleRepository;
import com.sgivu.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(userRepository, roleRepository, passwordEncoder);
  }

  @Test
  void saveShouldNormalizeRolesAndEncodePassword() {
    Role adminRole = ServiceTestDataProvider.role(1L, "ADMIN");
    User newUser = ServiceTestDataProvider.user(null, "jdoe", "raw-secret", Set.of(adminRole));

    when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
    when(passwordEncoder.encode("raw-secret")).thenReturn("encoded-secret");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.save(newUser);

    assertThat(result.getPassword()).isEqualTo("encoded-secret");
    assertThat(result.getRoles()).containsExactly(adminRole);
    verify(roleRepository).findByName("ADMIN");
    verify(userRepository).save(result);
  }

  @Test
  void saveShouldAssignDefaultRoleWhenNoAuthoritiesProvided() {
    Role defaultRole = ServiceTestDataProvider.role(2L, "USER");
    User newUser = ServiceTestDataProvider.user(null, "msmith", "plain", Set.of());

    when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));
    when(passwordEncoder.encode("plain")).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.save(newUser);

    assertThat(result.getRoles()).containsExactly(defaultRole);
    assertThat(result.getPassword()).isEqualTo("hashed");
    verify(roleRepository).findByName("USER");
  }

  @Test
  void updateShouldApplyChangesWhenUserExistsAndPasswordProvided() {
    Role userRole = ServiceTestDataProvider.role(3L, "USER");
    User existing = ServiceTestDataProvider.user(5L, "olduser", "old-hash", Set.of(userRole));
    UserUpdateRequest request =
        ServiceTestDataProvider.userUpdateRequest(
            "Alice",
            "Smith",
            "alice.smith@example.com",
            "asmith",
            "new-password",
            Set.of(userRole));

    when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
    when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
    when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<User> result = userService.update(5L, request);

    assertThat(result).isPresent();
    User updated = result.orElseThrow();
    assertThat(updated.getFirstName()).isEqualTo("Alice");
    assertThat(updated.getLastName()).isEqualTo("Smith");
    assertThat(updated.getEmail()).isEqualTo("alice.smith@example.com");
    assertThat(updated.getUsername()).isEqualTo("asmith");
    assertThat(updated.getPassword()).isEqualTo("encoded-new");
    assertThat(updated.getRoles()).containsExactly(userRole);
    verify(userRepository).save(existing);
  }

  @Test
  void updateShouldReturnEmptyWhenUserNotFound() {
    UserUpdateRequest request =
        ServiceTestDataProvider.userUpdateRequest(
            "Alice", "Smith", "alice.smith@example.com", "asmith", "new-password", Set.of());

    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<User> result = userService.update(99L, request);

    assertThat(result).isEmpty();
    verify(userRepository, never()).save(any(User.class));
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  void updateShouldKeepExistingPasswordWhenBlank() {
    Role userRole = ServiceTestDataProvider.role(4L, "USER");
    User existing = ServiceTestDataProvider.user(6L, "jbloggs", "existing-hash", Set.of(userRole));
    UserUpdateRequest request =
        ServiceTestDataProvider.userUpdateRequest(
            "Joe", "Bloggs", "joe.bloggs@example.com", "jbloggs", "", Set.of(userRole));

    when(userRepository.findById(6L)).thenReturn(Optional.of(existing));
    when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<User> result = userService.update(6L, request);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getPassword()).isEqualTo("existing-hash");
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  void changeStatusShouldUpdateWhenUserExists() {
    User existing = ServiceTestDataProvider.user(7L, "lockuser", "hash", Set.of());
    existing.setEnabled(false);

    when(userRepository.findById(7L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    boolean updated = userService.changeStatus(7L, true);

    assertThat(updated).isTrue();
    assertThat(existing.isEnabled()).isTrue();
    verify(userRepository).save(existing);
  }

  @Test
  void changeStatusShouldReturnFalseWhenUserMissing() {
    when(userRepository.findById(100L)).thenReturn(Optional.empty());

    boolean updated = userService.changeStatus(100L, false);

    assertThat(updated).isFalse();
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void searchShouldDelegateToRepositoryWithSpecification() {
    UserFilterCriteria criteria = UserFilterCriteria.builder().name("john").build();
    User matchingUser = ServiceTestDataProvider.user(8L, "lookup", "hash", Set.of());
    Pageable pageable = PageRequest.of(0, 5);
    Page<User> page = new PageImpl<>(java.util.List.of(matchingUser));

    when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable)))
        .thenReturn(page);

    Page<User> result = userService.search(criteria, pageable);

    assertThat(result.getContent()).containsExactly(matchingUser);
    verify(userRepository).findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable));
  }
}
