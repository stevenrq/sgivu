package com.sgivu.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table(name = "persons")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Person implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "persons_id_seq")
  @SequenceGenerator(name = "persons_id_seq", sequenceName = "persons_id_seq", allocationSize = 1)
  private Long id;

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

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "address_id", referencedColumnName = "id")
  private Address address;

  @NotNull
  @Column(name = "phone_number", nullable = false, unique = true)
  private Long phoneNumber;

  @Email
  @Size(min = 16, max = 40)
  @NotBlank
  @Column(nullable = false, unique = true, length = 40)
  private String email;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();

    if (this instanceof User user) {
      user.prePersistUser();
    }
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
