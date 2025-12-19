package com.sgivu.user;

import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.Permission;
import com.sgivu.user.entity.Role;
import com.sgivu.user.entity.User;
import java.util.HashSet;
import java.util.Set;

/** Utilidades ligeras para construir fixtures reutilizables en pruebas de servicios. */
public final class ServiceTestDataProvider {

  private ServiceTestDataProvider() {}

  public static Role role(Long id, String name) {
    Role role = new Role();
    role.setId(id);
    role.setName(name);
    role.setPermissions(new HashSet<>());
    return role;
  }

  public static Permission permission(Long id, String name) {
    Permission permission = new Permission();
    permission.setId(id);
    permission.setName(name);
    return permission;
  }

  public static User user(Long id, String username, String password, Set<Role> roles) {
    User user = new User();
    user.setId(id);
    user.setNationalId(1098765432L);
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setPhoneNumber(3100000000L);
    user.setEmail(username + "@example.com");
    user.setUsername(username);
    user.setPassword(password);
    user.setEnabled(true);
    user.setAccountNonExpired(true);
    user.setAccountNonLocked(true);
    user.setCredentialsNonExpired(true);
    user.setRoles(roles == null ? new HashSet<>() : new HashSet<>(roles));
    return user;
  }

  public static UserUpdateRequest userUpdateRequest(
      String firstName,
      String lastName,
      String email,
      String username,
      String password,
      Set<Role> roles) {
    UserUpdateRequest request = new UserUpdateRequest();
    request.setFirstName(firstName);
    request.setLastName(lastName);
    request.setPhoneNumber(3110000000L);
    request.setEmail(email);
    request.setUsername(username);
    request.setPassword(password);
    request.setRoles(roles);
    return request;
  }
}
