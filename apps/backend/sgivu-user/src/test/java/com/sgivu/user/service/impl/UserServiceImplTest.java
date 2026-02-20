package com.sgivu.user.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.Address;
import com.sgivu.user.entity.Role;
import com.sgivu.user.entity.User;
import com.sgivu.user.exception.RoleRetrievalException;
import com.sgivu.user.repository.RoleRepository;
import com.sgivu.user.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("save(User)")
  class SaveUserTests {
    @Test
    @DisplayName("Debe codificar contraseña, asignar roles y guardar usuario")
    void shouldEncodePasswordAndSaveUser() {
      User user = new User();
      user.setPassword("plainPassword");
      user.setRoles(new HashSet<>(Collections.singletonList(new Role())));

      when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
      when(userRepository.save(any(User.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User savedUser = userService.save(user);

      assertEquals("encodedPassword", savedUser.getPassword());
      verify(passwordEncoder).encode("plainPassword");
      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Debe asignar rol USER por defecto al guardar usuario")
    void shouldAssignUserRoleByDefault() {
      User user = new User();
      user.setPassword("plainPassword");
      Role userRole = new Role();
      userRole.setName("USER");
      user.setRoles(new HashSet<>(Collections.singletonList(userRole)));

      when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
      when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
      when(userRepository.save(any(User.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      User savedUser = userService.save(user);

      assertNotNull(savedUser.getRoles());
      assertEquals(1, savedUser.getRoles().size());
      assertTrue(savedUser.getRoles().stream().anyMatch(role -> "USER".equals(role.getName())));
      assertEquals("encodedPassword", savedUser.getPassword());
      verify(roleRepository).findByName("USER");
      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Debe lanzar NullPointerException si la contraseña es nula")
    void shouldThrowNullPointerExceptionIfPasswordIsNull() {
      User user = new User();
      user.setRoles(new HashSet<>(Collections.singletonList(new Role())));
      user.setPassword(null);
      assertThrows(NullPointerException.class, () -> userService.save(user));
    }

    @Test
    @DisplayName("Debe lanzar RoleRetrievalException si el rol no se encuentra")
    void shouldThrowRoleRetrievalExceptionIfRoleNotFound() {
      User user = new User();
      user.setPassword("somePassword");
      user.setRoles(Collections.emptySet());

      when(roleRepository.findByName(any())).thenReturn(Optional.empty());

      assertThrows(RoleRetrievalException.class, () -> userService.save(user));
      verify(roleRepository).findByName(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el repositorio falla")
    void shouldThrowExceptionIfRepositoryFails() {
      User user = new User();
      user.setPassword("plainPassword");
      user.setRoles(new HashSet<>(Collections.singletonList(new Role())));

      when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
      when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> userService.save(user));
      verify(userRepository).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("update(Long, UserUpdateRequest)")
  class UpdateUserTests {
    @Test
    @DisplayName("Debe actualizar campos del usuario y codificar contraseña si se proporciona")
    void shouldUpdateUserAndEncodePasswordIfProvided() {
      Long userId = 1L;
      User existingUser = new User();
      existingUser.setId(userId);
      existingUser.setPassword("oldPassword");
      Role userRole = new Role();
      userRole.setName("USER");
      existingUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));

      UserUpdateRequest updateRequest = new UserUpdateRequest();
      updateRequest.setFirstName("NewName");
      updateRequest.setLastName("NewLast");
      updateRequest.setAddress(new Address());
      updateRequest.setPhoneNumber(123456789L);
      updateRequest.setEmail("new@email.com");
      updateRequest.setUsername("newuser");
      updateRequest.setPassword("newPassword");

      when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
      when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
      when(userRepository.save(any(User.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<User> result = userService.update(userId, updateRequest);

      assertTrue(result.isPresent());
      User updatedUser = result.get();
      assertEquals("NewName", updatedUser.getFirstName());
      assertEquals("NewLast", updatedUser.getLastName());
      assertNotNull(updatedUser.getAddress());
      assertEquals(123456789, updatedUser.getPhoneNumber());
      assertEquals("new@email.com", updatedUser.getEmail());
      assertEquals("newuser", updatedUser.getUsername());
      assertEquals("encodedNewPassword", updatedUser.getPassword());
      verify(passwordEncoder).encode("newPassword");
      verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Debe actualizar campos del usuario y conservar contraseña si no se proporciona")
    void shouldUpdateUserAndKeepPasswordIfNotProvided() {
      Long userId = 2L;
      User existingUser = new User();
      existingUser.setId(userId);
      existingUser.setPassword("oldPassword");
      Role userRole = new Role();
      userRole.setName("USER");
      existingUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));

      UserUpdateRequest updateRequest = new UserUpdateRequest();
      updateRequest.setFirstName("Name");
      updateRequest.setLastName("Last");
      updateRequest.setAddress(new Address());
      updateRequest.setPhoneNumber(987654321L);
      updateRequest.setEmail("email@test.com");
      updateRequest.setUsername("user");
      updateRequest.setPassword("");

      when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
      when(userRepository.save(any(User.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<User> result = userService.update(userId, updateRequest);

      assertTrue(result.isPresent());
      User updatedUser = result.get();
      assertEquals("oldPassword", updatedUser.getPassword());
      verify(passwordEncoder, never()).encode(any());
      verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío si el usuario no se encuentra")
    void shouldReturnEmptyOptionalIfUserNotFound() {
      Long userId = 99L;
      UserUpdateRequest updateRequest = new UserUpdateRequest();
      when(userRepository.findById(userId)).thenReturn(Optional.empty());
      Optional<User> result = userService.update(userId, updateRequest);
      assertFalse(result.isPresent());
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el repositorio falla")
    void shouldThrowExceptionIfRepositoryFails() {
      Long userId = 3L;
      User existingUser = new User();
      existingUser.setId(userId);
      existingUser.setPassword("oldPassword");
      existingUser.setRoles(Collections.emptySet());
      Role userRole = new Role();
      userRole.setName("USER");

      UserUpdateRequest updateRequest = new UserUpdateRequest();
      updateRequest.setFirstName("Name");
      updateRequest.setLastName("Last");
      updateRequest.setAddress(new Address());
      updateRequest.setPhoneNumber(987654321L);
      updateRequest.setEmail("email@test.com");
      updateRequest.setUsername("user");
      updateRequest.setPassword(null);

      when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
      when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
      when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> userService.update(userId, updateRequest));
      verify(userRepository).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("changeStatus(Long, boolean)")
  class ChangeStatusTests {
    @Test
    @DisplayName("Debe habilitar usuario cuando se encuentra")
    void shouldEnableUserWhenFound() {
      Long userId = 1L;
      User user = new User();
      user.setId(userId);
      user.setEnabled(false);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(userRepository.save(any(User.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      boolean result = userService.changeStatus(userId, true);

      assertTrue(result);
      assertTrue(user.isEnabled());
      verify(userRepository).findById(userId);
      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Debe deshabilitar usuario cuando se encuentra")
    void shouldDisableUserWhenFound() {
      Long userId = 2L;
      User user = new User();
      user.setId(userId);
      user.setEnabled(true);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(userRepository.save(any(User.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      boolean result = userService.changeStatus(userId, false);

      assertTrue(result);
      assertFalse(user.isEnabled());
      verify(userRepository).findById(userId);
      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Debe retornar false si el usuario no se encuentra")
    void shouldReturnFalseIfUserNotFound() {
      Long userId = 99L;

      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      boolean result = userService.changeStatus(userId, true);
      assertFalse(result);
      verify(userRepository).findById(userId);
      verify(userRepository, never()).save(any(User.class));
    }
  }
}
