package com.sgivu.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Rol funcional dentro de SGIVU (ej. ADMIN, ANALISTA, VENTAS).
 *
 * <p>Define un conjunto de permisos y se utiliza para autorizar operaciones transaccionales en
 * ventas de vehículos usados, contratos y predicciones de demanda.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roles_id_seq")
  @SequenceGenerator(name = "roles_id_seq", sequenceName = "roles_id_seq", allocationSize = 1)
  private Long id;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(nullable = false, unique = true, length = 20)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @EqualsAndHashCode.Exclude
  @JsonIgnoreProperties({"handler", "hibernateLazyInitializer"})
  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {
        CascadeType.DETACH,
        CascadeType.MERGE,
        CascadeType.PERSIST,
        CascadeType.REFRESH,
      })
  @JoinTable(
      name = "roles_permissions",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  private Set<Permission> permissions = new HashSet<>();

  /** Define marcas de tiempo iniciales al crear un rol. */
  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  /** Actualiza la fecha de modificación antes de persistir cambios. */
  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Agrega permisos al conjunto existente evitando duplicados gracias a la estructura {@link Set}.
   *
   * @param permissions colección de permisos a anexar.
   */
  public void addPermissions(Set<Permission> permissions) {
    this.getPermissions().addAll(permissions);
  }
}
