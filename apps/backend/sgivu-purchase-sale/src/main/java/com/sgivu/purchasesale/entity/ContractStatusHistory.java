package com.sgivu.purchasesale.entity;

import com.sgivu.purchasesale.enums.ContractStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "contract_status_history")
public class ContractStatusHistory implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_status_history_id_seq")
  @SequenceGenerator(
      name = "contract_status_history_id_seq",
      sequenceName = "contract_status_history_id_seq",
      allocationSize = 1)
  private Long id;

  @NotNull
  @Column(name = "purchase_sale_id", nullable = false)
  private Long purchaseSaleId;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = 50)
  private ContractStatus previousStatus;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 50)
  private ContractStatus newStatus;

  @Column(name = "changed_by")
  private Long changedBy;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private LocalDateTime changedAt;

  @Column(length = 300)
  private String reason;

  @PrePersist
  void onCreate() {
    if (this.changedAt == null) {
      this.changedAt = LocalDateTime.now();
    }
  }
}
