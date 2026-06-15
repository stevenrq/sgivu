package com.sgivu.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "cars")
@PrimaryKeyJoinColumn(name = "vehicle_id", referencedColumnName = "id")
public class Car extends Vehicle {
  @Serial private static final long serialVersionUID = 1L;

  @NotBlank
  @Column(name = "body_type", nullable = false, length = 20)
  private String bodyType;

  @NotBlank
  @Column(name = "fuel_type", nullable = false, length = 20)
  private String fuelType;

  @NotNull
  @Column(name = "number_of_doors", nullable = false)
  private Integer numberOfDoors;
}
