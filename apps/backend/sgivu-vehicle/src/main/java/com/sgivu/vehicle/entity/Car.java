package com.sgivu.vehicle.entity;

import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * Representa un automóvil en el inventario.
 *
 * <p>Complementa los atributos base con tipologías de carrocería y combustible, claves para validar
 * compatibilidad con contratos de garantía y segmentación de demanda.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "cars")
@PrimaryKeyJoinColumn(name = "vehicle_id", referencedColumnName = "id")
@io.swagger.v3.oas.annotations.media.Schema(
    description = "Entidad concreta Auto usada como request en endpoints de auto")
public class Car extends Vehicle {
  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "Tipo de carrocería", example = "Sedán")
  @NotBlank
  @Column(name = "body_type", nullable = false, length = 20)
  private String bodyType;

  @Schema(description = "Tipo de combustible", example = "Gasolina")
  @NotBlank
  @Column(name = "fuel_type", nullable = false, length = 20)
  private String fuelType;

  @Schema(description = "Número de puertas", example = "4")
  @NotNull
  @Column(name = "number_of_doors", nullable = false)
  private Integer numberOfDoors;
}
