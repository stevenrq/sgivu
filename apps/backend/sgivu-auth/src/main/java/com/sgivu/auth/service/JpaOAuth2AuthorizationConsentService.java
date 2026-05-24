package com.sgivu.auth.service;

import com.sgivu.auth.entity.AuthorizationConsent;
import com.sgivu.auth.repository.AuthorizationConsentRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class JpaOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

  private final AuthorizationConsentRepository authorizationConsentRepository;
  private final RegisteredClientRepository registeredClientRepository;

  public JpaOAuth2AuthorizationConsentService(
      AuthorizationConsentRepository authorizationConsentRepository,
      RegisteredClientRepository registeredClientRepository) {
    Assert.notNull(
        authorizationConsentRepository, "authorizationConsentRepository no puede ser nulo");
    Assert.notNull(registeredClientRepository, "registeredClientRepository no puede ser nulo");
    this.authorizationConsentRepository = authorizationConsentRepository;
    this.registeredClientRepository = registeredClientRepository;
  }

  @Override
  @Transactional
  public void save(OAuth2AuthorizationConsent authorizationConsent) {
    Assert.notNull(authorizationConsent, "authorizationConsent no puede ser nulo");
    this.authorizationConsentRepository.save(toEntity(authorizationConsent));
  }

  @Override
  @Transactional
  public void remove(OAuth2AuthorizationConsent authorizationConsent) {
    Assert.notNull(authorizationConsent, "authorizationConsent no puede ser nulo");
    this.authorizationConsentRepository.deleteByRegisteredClientIdAndPrincipalName(
        authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
  }

  @Override
  @Transactional(readOnly = true)
  public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
    Assert.hasText(registeredClientId, "registeredClientId no puede estar vacío");
    Assert.hasText(principalName, "principalName no puede estar vacío");
    return this.authorizationConsentRepository
        .findByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
        .map(this::toObject)
        .orElse(null);
  }

  private OAuth2AuthorizationConsent toObject(AuthorizationConsent authorizationConsent) {
    String registeredClientId = authorizationConsent.getRegisteredClientId();
    RegisteredClient registeredClient =
        this.registeredClientRepository.findById(registeredClientId);
    if (registeredClient == null) {
      throw new DataRetrievalFailureException(
          "El RegisteredClient con id '"
              + registeredClientId
              + "' no fue encontrado en el RegisteredClientRepository.");
    }

    OAuth2AuthorizationConsent.Builder builder =
        OAuth2AuthorizationConsent.withId(
            registeredClientId, authorizationConsent.getPrincipalName());
    if (authorizationConsent.getAuthorities() != null) {
      for (String authority :
          StringUtils.commaDelimitedListToSet(authorizationConsent.getAuthorities())) {
        builder.authority(new SimpleGrantedAuthority(authority));
      }
    }

    return builder.build();
  }

  private AuthorizationConsent toEntity(OAuth2AuthorizationConsent authorizationConsent) {
    AuthorizationConsent entity = new AuthorizationConsent();
    entity.setRegisteredClientId(authorizationConsent.getRegisteredClientId());
    entity.setPrincipalName(authorizationConsent.getPrincipalName());

    Set<String> authorities = new HashSet<>();
    for (GrantedAuthority authority : authorizationConsent.getAuthorities()) {
      authorities.add(authority.getAuthority());
    }
    entity.setAuthorities(StringUtils.collectionToCommaDelimitedString(authorities));

    return entity;
  }
}
