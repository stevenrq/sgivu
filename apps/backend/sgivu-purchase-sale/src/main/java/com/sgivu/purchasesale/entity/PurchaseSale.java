package com.sgivu.purchasesale.entity;

import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA que representa contratos de compra o venta de vehículos usados. Persistimos únicamente
 * identificadores de cliente, usuario y vehículo para mantener bajo acoplamiento con microservicios
 * maestros, mientras que los precios y condiciones financieras se mantienen localmente para
 * auditoría y reportes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "purchase_sales")
public class PurchaseSale implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_sales_id_seq")
  @SequenceGenerator(
      name = "purchase_sales_id_seq",
      sequenceName = "purchase_sales_id_seq",
      allocationSize = 1)
  private Long id;

  private Long clientId;

  private Long userId;

  private Long vehicleId;

  @NotNull
  @PositiveOrZero
  @Column(name = "purchase_price")
  private Double purchasePrice;

  @NotNull
  @PositiveOrZero
  @Column(name = "sale_price")
  private Double salePrice;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "contract_type", nullable = false)
  private ContractType contractType;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "contract_status", nullable = false)
  private ContractStatus contractStatus;

  @NotBlank
  @Column(name = "payment_limitations", nullable = false, length = 200)
  private String paymentLimitations;

  @NotBlank
  @Column(name = "payment_terms", nullable = false, length = 200)
  private String paymentTerms;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private PaymentMethod paymentMethod;

  @Column(name = "observations", length = 500)
  private String observations;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Inicializa marcas de tiempo antes de persistir. Se fuerza updatedAt para que reportes y
   * proyecciones utilicen un valor consistente en la primera escritura.
   */
  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = Objects.requireNonNullElseGet(this.createdAt, () -> now);
    this.updatedAt = now;
  }

  /** Actualiza la marca temporal de modificación para auditorías. */
  @PreUpdate
  void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
