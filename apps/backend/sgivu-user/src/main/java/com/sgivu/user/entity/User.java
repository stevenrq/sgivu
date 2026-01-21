package com.sgivu.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sgivu.user.util.RolePermissionUtils;
import com.sgivu.user.validation.ValidationGroups.Create;
import com.sgivu.user.validation.annotation.NoSpecialCharacters;
import com.sgivu.user.validation.annotation.PasswordStrength;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
@PrimaryKeyJoinColumn(name = "person_id", referencedColumnName = "id")
public class User extends Person {

  @Serial private static final long serialVersionUID = 1L;

  @Size(min = 6, max = 20)
  @NoSpecialCharacters
  @NotBlank
  @Column(nullable = false, unique = true, length = 20)
  private String username;

  @Size(min = 6, max = 60, groups = Create.class)
  @PasswordStrength(groups = Create.class)
  @NotBlank(groups = Create.class)
  @Column(nullable = false, length = 60)
  private String password;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "account_non_expired", nullable = false)
  private boolean accountNonExpired;

  @Column(name = "account_non_locked", nullable = false)
  private boolean accountNonLocked;

  @Column(name = "credentials_non_expired", nullable = false)
  private boolean credentialsNonExpired;

  @Transient private boolean admin;

  @JsonIgnoreProperties({"handler", "hibernateLazyInitializer"})
  @EqualsAndHashCode.Exclude
  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {
        CascadeType.DETACH,
        CascadeType.MERGE,
        CascadeType.PERSIST,
        CascadeType.REFRESH,
      })
  @JoinTable(
      name = "users_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  public void prePersistUser() {
    this.enabled = true;
    this.accountNonExpired = true;
    this.accountNonLocked = true;
    this.credentialsNonExpired = true;
  }

  public Set<String> getRolesAndPermissions() {
    return RolePermissionUtils.getRolesAndPermissions(this.roles);
  }
}
