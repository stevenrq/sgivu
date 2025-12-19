package com.sgivu.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.user.ServiceTestDataProvider;
import com.sgivu.user.entity.Permission;
import com.sgivu.user.repository.PermissionRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

  @Mock private PermissionRepository permissionRepository;

  private PermissionServiceImpl permissionService;

  @BeforeEach
  void setUp() {
    permissionService = new PermissionServiceImpl(permissionRepository);
  }

  @Test
  void findByNameInShouldReturnMatchingPermissions() {
    Set<String> names = Set.of("user:view", "user:edit");
    Set<Permission> permissions =
        Set.of(
            ServiceTestDataProvider.permission(1L, "user:view"),
            ServiceTestDataProvider.permission(2L, "user:edit"));

    when(permissionRepository.findByNameIn(names)).thenReturn(Optional.of(permissions));

    Optional<Set<Permission>> result = permissionService.findByNameIn(names);

    assertThat(result).contains(permissions);
    verify(permissionRepository).findByNameIn(names);
  }

  @Test
  void findAllShouldReturnCatalog() {
    List<Permission> permissions = List.of(ServiceTestDataProvider.permission(3L, "user:list"));

    when(permissionRepository.findAll()).thenReturn(permissions);

    List<Permission> result = permissionService.findAll();

    assertThat(result).isEqualTo(permissions);
    verify(permissionRepository).findAll();
  }
}
