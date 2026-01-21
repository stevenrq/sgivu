package com.sgivu.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "addresses")
public class Address implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(min = 5, max = 100)
  @Column(nullable = false, length = 100)
  private String street;

  @NotBlank
  @Size(min = 1, max = 20)
  @Column(nullable = false, length = 20)
  private String number;

  @NotBlank
  @Size(min = 3, max = 50)
  @Column(nullable = false, length = 50)
  private String city;
}
