package com.sgivu.auth.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "authorization_consents")
@IdClass(AuthorizationConsent.AuthorizationConsentId.class)
public class AuthorizationConsent {

  @Id
  @Column(name = "registered_client_id")
  private String registeredClientId;

  @Id
  @Column(name = "principal_name")
  private String principalName;

  @Column(name = "authorities", length = 1000)
  private String authorities;

  @Data
  @NoArgsConstructor
  public static class AuthorizationConsentId implements Serializable {

    private String registeredClientId;

    private String principalName;
  }
}
