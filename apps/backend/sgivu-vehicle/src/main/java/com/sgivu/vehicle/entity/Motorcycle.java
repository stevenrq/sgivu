package com.sgivu.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "motorcycles")
@PrimaryKeyJoinColumn(name = "vehicle_id", referencedColumnName = "id")
public class Motorcycle extends Vehicle {
  @Serial private static final long serialVersionUID = 1L;

  @NotBlank
  @Column(name = "motorcycle_type", nullable = false, length = 20)
  private String motorcycleType;
}
