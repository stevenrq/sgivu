package com.sgivu.client.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cliente empresa para operaciones corporativas de SGIVU. Almacena identificador fiscal y razón
 * social usados en compras/ventas y contratos.
 *
 * <p>Se mantiene la relación con {@link Client} para reutilizar domicilio y contacto al integrar
 * con facturación y auditorías.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "companies")
@PrimaryKeyJoinColumn(name = "client_id", referencedColumnName = "id")
public class Company extends Client {
  @Serial private static final long serialVersionUID = 1L;

  @NotEmpty
  @Column(name = "tax_id", nullable = false, unique = true)
  private String taxId;

  @NotEmpty
  @Column(name = "company_name", nullable = false, unique = true)
  private String companyName;
}
