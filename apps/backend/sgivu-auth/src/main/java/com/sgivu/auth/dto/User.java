package com.sgivu.auth.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class User {

  private Long id;
  private String username;
  private String password;
  private boolean enabled;
  private boolean accountNonExpired;
  private boolean accountNonLocked;
  private boolean credentialsNonExpired;
  private Set<Role> roles = new HashSet<>();

  public Set<String> getRolesAndPermissions() {
    return roles.stream()
        .flatMap(
            role ->
                Stream.concat(
                    Stream.of(role.getName()),
                    role.getPermissions() == null
                        ? Stream.empty()
                        : role.getPermissions().stream().map(Permission::getName)))
        .collect(Collectors.toSet());
  }
}
