package com.sgivu.vehicle.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadatos de imágenes asociadas a un vehículo.
 *
 * <p>Se persiste solo la referencia al objeto en S3, permitiendo auditoría de uploads y control de
 * imagen principal usada por los portales de venta SGIVU.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_images")
public class VehicleImage {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vehicle_images_id_seq")
  @SequenceGenerator(
      name = "vehicle_images_id_seq",
      sequenceName = "vehicle_images_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  @Column(nullable = false, length = 100)
  private String bucket;

  @Column(nullable = false, unique = true)
  private String key;

  @Column(name = "file_name", nullable = false, unique = true)
  private String fileName;

  @Column(name = "mime_type", length = 100)
  private String mimeType;

  @Column(name = "file_size")
  private Long size;

  @Column(name = "is_primary", nullable = false)
  private boolean primaryImage = false;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  /** Registra fecha de creación al guardar por primera vez. */
  @PrePersist
  void prePersist() {
    this.createdAt = LocalDateTime.now();
  }
}
