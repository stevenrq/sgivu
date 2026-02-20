package com.sgivu.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "clients")
public class Client {

  @Id private String id;

  @Column(name = "client_id")
  private String clientId;

  @Column(name = "client_id_issued_at")
  private Instant clientIdIssuedAt;

  @Column(name = "client_secret")
  private String clientSecret;

  @Column(name = "client_secret_expires_at")
  private Instant clientSecretExpiresAt;

  @Column(name = "client_name")
  private String clientName;

  @Column(name = "client_authentication_methods", length = 1000)
  private String clientAuthenticationMethods;

  @Column(name = "authorization_grant_types", length = 1000)
  private String authorizationGrantTypes;

  @Column(name = "redirect_uris", length = 1000)
  private String redirectUris;

  @Column(name = "post_logout_redirect_uris", length = 1000)
  private String postLogoutRedirectUris;

  @Column(name = "scopes", length = 1000)
  private String scopes;

  @Column(name = "client_settings", length = 2000)
  private String clientSettings;

  @Column(name = "token_settings", length = 2000)
  private String tokenSettings;
}
