package com.sgivu.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.user.ServiceTestDataProvider;
import com.sgivu.user.entity.Permission;
import com.sgivu.user.entity.Role;
import com.sgivu.user.repository.RoleRepository;
import com.sgivu.user.service.PermissionService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

  @Mock private RoleRepository roleRepository;
  @Mock private PermissionService permissionService;

  private RoleServiceImpl roleService;

  @BeforeEach
  void setUp() {
    roleService = new RoleServiceImpl(roleRepository, permissionService);
  }

  @Test
  void addPermissionsShouldAppendFoundPermissions() {
    Role role = ServiceTestDataProvider.role(1L, "ADMIN");
    Permission existing = ServiceTestDataProvider.permission(1L, "user:read");
    role.getPermissions().add(existing);
    Permission newPermission = ServiceTestDataProvider.permission(2L, "user:create");

    Set<String> namesToAdd = Set.of("user:create");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
    when(permissionService.findByNameIn(namesToAdd)).thenReturn(Optional.of(Set.of(newPermission)));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Role> result = roleService.addPermissions(1L, namesToAdd);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getPermissions()).contains(existing, newPermission);
    verify(permissionService).findByNameIn(namesToAdd);
    verify(roleRepository).save(role);
  }

  @Test
  void addPermissionsShouldReturnExistingRoleWhenNamesEmpty() {
    Role role = ServiceTestDataProvider.role(2L, "ANALYST");

    when(roleRepository.findById(2L)).thenReturn(Optional.of(role));

    Optional<Role> result = roleService.addPermissions(2L, Set.of());

    assertThat(result).contains(role);
    verify(roleRepository, never()).save(any(Role.class));
    verify(permissionService, never()).findByNameIn(any());
  }

  @Test
  void removePermissionsShouldDropProvidedOnes() {
    Role role = ServiceTestDataProvider.role(3L, "SUPPORT");
    Permission removable = ServiceTestDataProvider.permission(3L, "user:update");
    Permission remaining = ServiceTestDataProvider.permission(4L, "user:view");
    role.getPermissions().addAll(Set.of(removable, remaining));

    Set<String> namesToRemove = Set.of("user:update");

    when(roleRepository.findById(3L)).thenReturn(Optional.of(role));
    when(permissionService.findByNameIn(namesToRemove)).thenReturn(Optional.of(Set.of(removable)));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Role> result = roleService.removePermissions(3L, namesToRemove);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getPermissions()).containsExactly(remaining);
    verify(permissionService).findByNameIn(namesToRemove);
    verify(roleRepository).save(role);
  }

  @Test
  void updatePermissionsShouldClearWhenNamesNull() {
    Role role = ServiceTestDataProvider.role(4L, "VIEWER");
    role.getPermissions().add(ServiceTestDataProvider.permission(5L, "user:read"));

    when(roleRepository.findById(4L)).thenReturn(Optional.of(role));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Role> result = roleService.updatePermissions(4L, null);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getPermissions()).isEmpty();
    verify(permissionService, never()).findByNameIn(any());
    verify(roleRepository).save(role);
  }

  @Test
  void updatePermissionsShouldReplaceWithNewOnes() {
    Role role = ServiceTestDataProvider.role(5L, "MANAGER");
    role.getPermissions().add(ServiceTestDataProvider.permission(6L, "user:read"));

    Permission replacement = ServiceTestDataProvider.permission(7L, "user:approve");
    Set<String> requestedPermissions = Set.of("user:approve");

    when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
    when(permissionService.findByNameIn(requestedPermissions))
        .thenReturn(Optional.of(Set.of(replacement)));
    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Role> result = roleService.updatePermissions(5L, requestedPermissions);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getPermissions()).containsExactly(replacement);
    verify(permissionService).findByNameIn(requestedPermissions);
    verify(roleRepository).save(role);
  }
}
