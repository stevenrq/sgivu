package com.sgivu.user.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.user.entity.Permission;
import com.sgivu.user.entity.Role;
import com.sgivu.user.repository.RoleRepository;
import com.sgivu.user.service.PermissionService;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RoleServiceImplTest {

  @Mock private RoleRepository roleRepository;
  @Mock private PermissionService permissionService;

  @InjectMocks private RoleServiceImpl roleService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("addPermissions(Long, Set<String>)")
  class AddPermissionsTests {

    @Test
    @DisplayName("Debe retornar rol sin cambios si los nombres son nulos o vacíos")
    void shouldReturnRoleIfNamesNullOrEmpty() {
      Long roleId = 1L;
      Role role = new Role();
      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

      Optional<Role> resultNull = roleService.addPermissions(roleId, null);
      Optional<Role> resultEmpty = roleService.addPermissions(roleId, Collections.emptySet());

      assertTrue(resultNull.isPresent());
      assertTrue(resultEmpty.isPresent());
      assertSame(role, resultNull.get());
      assertSame(role, resultEmpty.get());
      verify(roleRepository, times(2)).findById(roleId);
      verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("Debe agregar permisos al rol cuando se encuentran")
    void shouldAddPermissionsToRole() {
      Long roleId = 2L;
      Set<String> names = new HashSet<>(Arrays.asList("user:read", "user:update"));
      Role role = spy(new Role());
      Set<Permission> permissions = new HashSet<>();
      Permission p1 = new Permission();
      Permission p2 = new Permission();
      permissions.add(p1);
      permissions.add(p2);

      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
      when(permissionService.findByNameIn(names)).thenReturn(Optional.of(permissions));
      when(roleRepository.save(role)).thenReturn(role);

      Optional<Role> result = roleService.addPermissions(roleId, names);

      assertTrue(result.isPresent());
      assertSame(role, result.get());
      verify(roleRepository).findById(roleId);
      verify(permissionService).findByNameIn(names);
      verify(role).addPermissions(permissions);
      verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Debe no agregar permisos si no se encuentran")
    void shouldNotAddPermissionsIfNoneFound() {
      Long roleId = 3L;
      Set<String> names = new HashSet<>(Collections.singletonList("permission:read"));
      Role role = spy(new Role());

      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
      when(permissionService.findByNameIn(names)).thenReturn(Optional.empty());
      when(roleRepository.save(role)).thenReturn(role);

      Optional<Role> result = roleService.addPermissions(roleId, names);

      assertTrue(result.isPresent());
      assertSame(role, result.get());
      verify(roleRepository).findById(roleId);
      verify(permissionService).findByNameIn(names);
      verify(role, never()).addPermissions(any());
      verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Debe retornar vacío si el rol no se encuentra")
    void shouldReturnEmptyIfRoleNotFound() {
      Long roleId = 4L;
      Set<String> names = new HashSet<>(Collections.singleton("user:read"));
      when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

      Optional<Role> result = roleService.addPermissions(roleId, names);

      assertFalse(result.isPresent());
      verify(roleRepository).findById(roleId);
      verifyNoInteractions(permissionService);
    }
  }

  @Nested
  @DisplayName("removePermissions(Long, Set<String>)")
  class RemovePermissionsTests {

    @Test
    @DisplayName("Debe retornar rol sin cambios si los nombres son nulos o vacíos")
    void shouldReturnRoleIfNamesNullOrEmpty() {
      Long roleId = 10L;
      Role role = new Role();
      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

      Optional<Role> resultNull = roleService.removePermissions(roleId, null);
      Optional<Role> resultEmpty = roleService.removePermissions(roleId, Collections.emptySet());

      assertTrue(resultNull.isPresent());
      assertTrue(resultEmpty.isPresent());
      assertSame(role, resultNull.get());
      assertSame(role, resultEmpty.get());
      verify(roleRepository, times(2)).findById(roleId);
      verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("Debe eliminar permisos del rol cuando se encuentran")
    void shouldRemovePermissionsFromRole() {
      Long roleId = 11L;
      Set<String> names = new HashSet<>(Arrays.asList("user:read", "user:update"));
      Role role = new Role();

      Permission p1 = new Permission();
      p1.setName("user:read");
      Permission p2 = new Permission();
      p2.setName("user:update");

      Set<Permission> existing = new HashSet<>(Arrays.asList(p1, p2));

      // Espiar los permisos establecidos para que podamos verificar que se llamó a removeAll
      Set<Permission> spySet = spy(new HashSet<>(existing));
      role.setPermissions(spySet);

      Set<Permission> toRemove = new HashSet<>(Collections.singletonList(p1));

      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
      when(permissionService.findByNameIn(names)).thenReturn(Optional.of(toRemove));
      when(roleRepository.save(role)).thenReturn(role);

      Optional<Role> result = roleService.removePermissions(roleId, names);

      assertTrue(result.isPresent());
      assertSame(role, result.get());
      verify(roleRepository).findById(roleId);
      verify(permissionService).findByNameIn(names);
      verify(spySet).removeAll(toRemove);
      verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Debe no eliminar permisos si no se encuentran")
    void shouldNotRemovePermissionsIfNoneFound() {
      Long roleId = 12L;
      Set<String> names = new HashSet<>(Collections.singletonList("permission:read"));
      Role role = new Role();

      Permission p1 = new Permission();
      p1.setName("permission:read");
      Set<Permission> existing = new HashSet<>(Collections.singletonList(p1));
      Set<Permission> spySet = spy(new HashSet<>(existing));
      role.setPermissions(spySet);

      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
      when(permissionService.findByNameIn(names)).thenReturn(Optional.empty());
      when(roleRepository.save(role)).thenReturn(role);

      Optional<Role> result = roleService.removePermissions(roleId, names);

      assertTrue(result.isPresent());
      assertSame(role, result.get());
      verify(roleRepository).findById(roleId);
      verify(permissionService).findByNameIn(names);
      verify(spySet, never()).removeAll(any());
      verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Debe retornar vacío si el rol no se encuentra")
    void shouldReturnEmptyIfRoleNotFound() {
      Long roleId = 13L;
      Set<String> names = new HashSet<>(Collections.singleton("user:read"));
      when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

      Optional<Role> result = roleService.removePermissions(roleId, names);

      assertFalse(result.isPresent());
      verify(roleRepository).findById(roleId);
      verifyNoInteractions(permissionService);
    }
  }

  @Nested
  @DisplayName("updatePermissions(Long, Set<String>)")
  class UpdatePermissionsTests {

    @Test
    @DisplayName("Debe limpiar permisos cuando los nombres son nulos o vacíos")
    void shouldClearPermissionsIfNamesNullOrEmpty() {
      Long roleId = 20L;
      Permission p1 = new Permission();
      p1.setName("person:read");
      Permission p2 = new Permission();
      p2.setName("person:update");

      Role role = new Role();
      Set<Permission> spySet = spy(new HashSet<>(Arrays.asList(p1, p2)));
      role.setPermissions(spySet);

      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
      when(roleRepository.save(role)).thenReturn(role);

      Optional<Role> resultNull = roleService.updatePermissions(roleId, null);
      Optional<Role> resultEmpty = roleService.updatePermissions(roleId, Collections.emptySet());

      assertTrue(resultNull.isPresent());
      assertTrue(resultEmpty.isPresent());
      assertSame(role, resultNull.get());
      assertSame(role, resultEmpty.get());
      verify(spySet, times(2)).clear();
      verifyNoInteractions(permissionService);
      verify(roleRepository, times(2)).findById(roleId);
      verify(roleRepository, times(2)).save(role);
    }

    @Test
    @DisplayName("Debe reemplazar permisos cuando se encuentran nuevos permisos")
    void shouldReplacePermissionsWhenFound() {
      Long roleId = 21L;
      Role role = new Role();
      Permission old = new Permission();
      old.setName("user:create");
      Set<Permission> spySet = spy(new HashSet<>(Collections.singletonList(old)));
      role.setPermissions(spySet);

      Permission a = new Permission();
      a.setName("company:read");
      Permission b = new Permission();
      b.setName("company:update");
      Set<Permission> newPermissions = new HashSet<>(Arrays.asList(a, b));

      Set<String> names = new HashSet<>(Arrays.asList("company:read", "company:update"));

      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
      when(permissionService.findByNameIn(names)).thenReturn(Optional.of(newPermissions));
      when(roleRepository.save(role)).thenReturn(role);

      Optional<Role> result = roleService.updatePermissions(roleId, names);

      assertTrue(result.isPresent());
      assertSame(role, result.get());
      verify(spySet).clear();
      verify(spySet).addAll(newPermissions);
      verify(permissionService).findByNameIn(names);
      verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Debe lanzar excepción si los permisos no se encuentran para los nombres")
    void shouldThrowIfPermissionsNotFound() {
      Long roleId = 22L;
      Role role = new Role();
      Set<Permission> spySet = spy(new HashSet<>());
      role.setPermissions(spySet);

      Set<String> names = new HashSet<>(Collections.singleton("missing:perm"));

      when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
      when(permissionService.findByNameIn(names)).thenReturn(Optional.empty());

      assertThrows(
          NoSuchElementException.class, () -> roleService.updatePermissions(roleId, names));
      verify(permissionService).findByNameIn(names);
      verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe retornar vacío si el rol no se encuentra")
    void shouldReturnEmptyIfRoleNotFound() {
      Long roleId = 23L;
      Set<String> names = new HashSet<>(Collections.singleton("user:read"));
      when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

      Optional<Role> result = roleService.updatePermissions(roleId, names);

      assertFalse(result.isPresent());
      verify(roleRepository).findById(roleId);
    }
  }
}
