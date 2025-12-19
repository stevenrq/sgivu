package com.sgivu.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Permiso granular que habilita acciones específicas (p. ej. {@code user:create},
 * {@code contract:approve}).
 *
 * <p>Se asocia a roles para controlar flujos distribuidos entre microservicios.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permissions_id_seq")
  @SequenceGenerator(
      name = "permissions_id_seq",
      sequenceName = "permissions_id_seq",
      allocationSize = 1)
  private Long id;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(nullable = false, unique = true, length = 20)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  /** Establece marcas de creación y actualización al insertar el permiso. */
  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  /** Refresca la fecha de actualización antes de guardar cambios. */
  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
