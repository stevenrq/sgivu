package com.sgivu.client.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cliente persona usado en SGIVU para representar compradores o vendedores de vehículos usados.
 * Extiende datos comunes y agrega identidad civil.
 *
 * <p>El {@code nationalId} se usa para deduplicar registros durante onboarding y contratos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "persons")
@PrimaryKeyJoinColumn(name = "client_id", referencedColumnName = "id")
public class Person extends Client {
  @Serial private static final long serialVersionUID = 1L;

  @NotNull
  @Column(name = "national_id", nullable = false, unique = true)
  private Long nationalId;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "first_name", nullable = false, length = 20)
  private String firstName;

  @Size(min = 3, max = 20)
  @NotBlank
  @Column(name = "last_name", nullable = false, length = 20)
  private String lastName;
}
