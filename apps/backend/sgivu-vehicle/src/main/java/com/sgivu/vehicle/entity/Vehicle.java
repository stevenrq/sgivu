package com.sgivu.vehicle.entity;

import com.sgivu.vehicle.enums.VehicleStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad base para vehículos usados administrados por SGIVU.
 *
 * <p>Centraliza los atributos comunes necesarios para los flujos de compra/venta y contratos
 * (placas únicas, números de motor/chasis para auditoría y trazabilidad). Se usa con herencia
 * JOINED para mantener coherencia en inventario y proyecciones JPA sobre autos y motos.
 *
 * @apiNote La estrategia JOINED permite consultas polimórficas sin duplicar columnas en tablas
 *     concretas, útil para reportes consolidados de inventario.
 */
@Data
@NoArgsConstructor
@Table(name = "vehicles")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@io.swagger.v3.oas.annotations.media.Schema(
    description = "Entidad base de vehículo usada como request en algunos endpoints")
public abstract class Vehicle implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vehicles_id_seq")
  @SequenceGenerator(name = "vehicles_id_seq", sequenceName = "vehicles_id_seq", allocationSize = 1)
  private Long id;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String brand;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String model;

  @NotNull
  @Column(nullable = false)
  private Integer capacity;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String line;

  @NotBlank
  @Column(nullable = false, unique = true, length = 10)
  private String plate;

  @NotBlank
  @Column(name = "motor_number", nullable = false, unique = true, length = 30)
  private String motorNumber;

  @NotBlank
  @Column(name = "serial_number", nullable = false, unique = true, length = 30)
  private String serialNumber;

  @NotBlank
  @Column(name = "chassis_number", nullable = false, unique = true, length = 30)
  private String chassisNumber;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String color;

  @NotBlank
  @Column(name = "city_registered", nullable = false, length = 30)
  private String cityRegistered;

  @Min(1950)
  @Max(2050)
  @NotNull
  @Column(nullable = false)
  private Integer year;

  @Min(0)
  @NotNull
  @Column(nullable = false)
  private Integer mileage;

  @NotBlank
  @Column(name = "transmission", nullable = false, length = 20)
  private String transmission;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VehicleStatus status;

  @PositiveOrZero
  @Column(name = "purchase_price", nullable = false)
  private Double purchasePrice;

  @PositiveOrZero
  @Column(name = "sale_price", nullable = false)
  private Double salePrice;

  @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<VehicleImage> images = new ArrayList<>();

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  /** Inicializa fechas de auditoría y asegura estado disponible por defecto. */
  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    if (this.status == null) this.status = VehicleStatus.AVAILABLE; // evita registrar sin estado
  }

  /** Actualiza fecha de modificación antes de persistir cambios. */
  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
