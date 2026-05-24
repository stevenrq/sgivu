package com.sgivu.auth.service;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgivu.auth.entity.Authorization;
import com.sgivu.auth.repository.AuthorizationRepository;
import com.sgivu.auth.security.CustomUserDetails;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class JpaOAuth2AuthorizationService implements OAuth2AuthorizationService {

  private final AuthorizationRepository authorizationRepository;
  private final RegisteredClientRepository registeredClientRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JpaOAuth2AuthorizationService(
      AuthorizationRepository authorizationRepository,
      RegisteredClientRepository registeredClientRepository) {
    Assert.notNull(authorizationRepository, "authorizationRepository no puede ser nulo");
    Assert.notNull(registeredClientRepository, "registeredClientRepository no puede ser nulo");
    this.authorizationRepository = authorizationRepository;
    this.registeredClientRepository = registeredClientRepository;

    ClassLoader classLoader = JpaOAuth2AuthorizationService.class.getClassLoader();
    List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
    this.objectMapper.registerModules(securityModules);
    this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());

    this.objectMapper.addMixIn(Long.class, Object.class);
    this.objectMapper.addMixIn(CustomUserDetails.class, CustomUserDetailsMixin.class);
  }

  private static AuthorizationGrantType resolveAuthorizationGrantType(
      String authorizationGrantType) {
    if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.AUTHORIZATION_CODE;
    } else if (AuthorizationGrantType.CLIENT_CREDENTIALS
        .getValue()
        .equals(authorizationGrantType)) {
      return AuthorizationGrantType.CLIENT_CREDENTIALS;
    } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.REFRESH_TOKEN;
    } else if (AuthorizationGrantType.DEVICE_CODE.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.DEVICE_CODE;
    }
    // Tipo de concesión de autorización personalizada
    return new AuthorizationGrantType(authorizationGrantType);
  }

  @Override
  @Transactional
  public void save(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization no puede ser nulo");
    this.authorizationRepository.save(this.toEntity(authorization));
  }

  @Override
  @Transactional
  public void remove(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization no puede ser nulo");
    this.authorizationRepository.deleteById(authorization.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public OAuth2Authorization findById(String id) {
    Assert.hasText(id, "id no puede estar vacío");
    return this.authorizationRepository.findById(id).map(this::toObject).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
    Assert.hasText(token, "token no puede estar vacío");

    Optional<Authorization> result;
    if (tokenType == null) {
      result =
          this.authorizationRepository
              .findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
                  token);
    } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
      result = this.authorizationRepository.findByState(token);
    } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
      result = this.authorizationRepository.findByAuthorizationCodeValue(token);
    } else if (OAuth2ParameterNames.ACCESS_TOKEN.equals(tokenType.getValue())) {
      result = this.authorizationRepository.findByAccessTokenValue(token);
    } else if (OAuth2ParameterNames.REFRESH_TOKEN.equals(tokenType.getValue())) {
      result = this.authorizationRepository.findByRefreshTokenValue(token);
    } else if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
      result = this.authorizationRepository.findByOidcIdTokenValue(token);
    } else if (OAuth2ParameterNames.USER_CODE.equals(tokenType.getValue())) {
      result = this.authorizationRepository.findByUserCodeValue(token);
    } else if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenType.getValue())) {
      result = this.authorizationRepository.findByDeviceCodeValue(token);
    } else {
      result = Optional.empty();
    }

    return result.map(this::toObject).orElse(null);
  }

  private OAuth2Authorization toObject(Authorization entity) {
    RegisteredClient registeredClient =
        this.registeredClientRepository.findById(entity.getRegisteredClientId());
    if (registeredClient == null) {
      throw new DataRetrievalFailureException(
          "El RegisteredClient con id '"
              + entity.getRegisteredClientId()
              + "' no fue encontrado en el RegisteredClientRepository.");
    }

    OAuth2Authorization.Builder builder =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id(entity.getId())
            .principalName(entity.getPrincipalName())
            .authorizationGrantType(
                resolveAuthorizationGrantType(entity.getAuthorizationGrantType()))
            .authorizedScopes(StringUtils.commaDelimitedListToSet(entity.getAuthorizedScopes()))
            .attributes(attributes -> attributes.putAll(parseMap(entity.getAttributes())));
    if (entity.getState() != null) {
      builder.attribute(OAuth2ParameterNames.STATE, entity.getState());
    }

    if (entity.getAuthorizationCodeValue() != null) {
      OAuth2AuthorizationCode authorizationCode =
          new OAuth2AuthorizationCode(
              entity.getAuthorizationCodeValue(),
              entity.getAuthorizationCodeIssuedAt(),
              entity.getAuthorizationCodeExpiresAt());
      builder.token(
          authorizationCode,
          metadata -> metadata.putAll(parseMap(entity.getAuthorizationCodeMetadata())));
    }

    if (entity.getAccessTokenValue() != null) {
      OAuth2AccessToken accessToken =
          new OAuth2AccessToken(
              OAuth2AccessToken.TokenType.BEARER,
              entity.getAccessTokenValue(),
              entity.getAccessTokenIssuedAt(),
              entity.getAccessTokenExpiresAt(),
              StringUtils.commaDelimitedListToSet(entity.getAccessTokenScopes()));
      builder.token(
          accessToken, metadata -> metadata.putAll(parseMap(entity.getAccessTokenMetadata())));
    }

    if (entity.getRefreshTokenValue() != null) {
      OAuth2RefreshToken refreshToken =
          new OAuth2RefreshToken(
              entity.getRefreshTokenValue(),
              entity.getRefreshTokenIssuedAt(),
              entity.getRefreshTokenExpiresAt());
      builder.token(
          refreshToken, metadata -> metadata.putAll(parseMap(entity.getRefreshTokenMetadata())));
    }

    if (entity.getOidcIdTokenValue() != null) {
      OidcIdToken idToken =
          new OidcIdToken(
              entity.getOidcIdTokenValue(),
              entity.getOidcIdTokenIssuedAt(),
              entity.getOidcIdTokenExpiresAt(),
              parseMap(entity.getOidcIdTokenClaims()));
      builder.token(
          idToken, metadata -> metadata.putAll(parseMap(entity.getOidcIdTokenMetadata())));
    }

    if (entity.getUserCodeValue() != null) {
      OAuth2UserCode userCode =
          new OAuth2UserCode(
              entity.getUserCodeValue(),
              entity.getUserCodeIssuedAt(),
              entity.getUserCodeExpiresAt());
      builder.token(userCode, metadata -> metadata.putAll(parseMap(entity.getUserCodeMetadata())));
    }

    if (entity.getDeviceCodeValue() != null) {
      OAuth2DeviceCode deviceCode =
          new OAuth2DeviceCode(
              entity.getDeviceCodeValue(),
              entity.getDeviceCodeIssuedAt(),
              entity.getDeviceCodeExpiresAt());
      builder.token(
          deviceCode, metadata -> metadata.putAll(parseMap(entity.getDeviceCodeMetadata())));
    }

    return builder.build();
  }

  private Authorization toEntity(OAuth2Authorization authorization) {
    Authorization entity = new Authorization();
    entity.setId(authorization.getId());
    entity.setRegisteredClientId(authorization.getRegisteredClientId());
    entity.setPrincipalName(authorization.getPrincipalName());
    entity.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());
    entity.setAuthorizedScopes(
        StringUtils.collectionToDelimitedString(authorization.getAuthorizedScopes(), ","));
    entity.setAttributes(writeMap(authorization.getAttributes()));
    entity.setState(authorization.getAttribute(OAuth2ParameterNames.STATE));

    OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
        authorization.getToken(OAuth2AuthorizationCode.class);
    setTokenValues(
        authorizationCode,
        entity::setAuthorizationCodeValue,
        entity::setAuthorizationCodeIssuedAt,
        entity::setAuthorizationCodeExpiresAt,
        entity::setAuthorizationCodeMetadata);

    OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
        authorization.getToken(OAuth2AccessToken.class);
    setTokenValues(
        accessToken,
        entity::setAccessTokenValue,
        entity::setAccessTokenIssuedAt,
        entity::setAccessTokenExpiresAt,
        entity::setAccessTokenMetadata);
    if (accessToken != null && accessToken.getToken().getScopes() != null) {
      entity.setAccessTokenScopes(
          StringUtils.collectionToDelimitedString(accessToken.getToken().getScopes(), ","));
    }

    OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
        authorization.getToken(OAuth2RefreshToken.class);
    setTokenValues(
        refreshToken,
        entity::setRefreshTokenValue,
        entity::setRefreshTokenIssuedAt,
        entity::setRefreshTokenExpiresAt,
        entity::setRefreshTokenMetadata);

    OAuth2Authorization.Token<OidcIdToken> oidcIdToken = authorization.getToken(OidcIdToken.class);
    setTokenValues(
        oidcIdToken,
        entity::setOidcIdTokenValue,
        entity::setOidcIdTokenIssuedAt,
        entity::setOidcIdTokenExpiresAt,
        entity::setOidcIdTokenMetadata);
    if (oidcIdToken != null) {
      entity.setOidcIdTokenClaims(writeMap(oidcIdToken.getClaims()));
    }

    OAuth2Authorization.Token<OAuth2UserCode> userCode =
        authorization.getToken(OAuth2UserCode.class);
    setTokenValues(
        userCode,
        entity::setUserCodeValue,
        entity::setUserCodeIssuedAt,
        entity::setUserCodeExpiresAt,
        entity::setUserCodeMetadata);

    OAuth2Authorization.Token<OAuth2DeviceCode> deviceCode =
        authorization.getToken(OAuth2DeviceCode.class);
    setTokenValues(
        deviceCode,
        entity::setDeviceCodeValue,
        entity::setDeviceCodeIssuedAt,
        entity::setDeviceCodeExpiresAt,
        entity::setDeviceCodeMetadata);

    return entity;
  }

  private void setTokenValues(
      OAuth2Authorization.Token<?> token,
      Consumer<String> tokenValueConsumer,
      Consumer<Instant> issuedAtConsumer,
      Consumer<Instant> expiresAtConsumer,
      Consumer<String> metadataConsumer) {
    if (token != null) {
      OAuth2Token oAuth2Token = token.getToken();
      tokenValueConsumer.accept(oAuth2Token.getTokenValue());
      issuedAtConsumer.accept(oAuth2Token.getIssuedAt());
      expiresAtConsumer.accept(oAuth2Token.getExpiresAt());
      metadataConsumer.accept(writeMap(token.getMetadata()));
    }
  }

  private Map<String, Object> parseMap(String data) {
    try {
      return this.objectMapper.readValue(data, new TypeReference<>() {});
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  private String writeMap(Map<String, Object> metadata) {
    try {
      return this.objectMapper.writeValueAsString(metadata);
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  /**
   * Mixin de Jackson para (de)serializar {@link com.sgivu.auth.security.CustomUserDetails}.
   *
   * <p>Se registra en el {@link ObjectMapper} de esta clase mediante {@code
   * objectMapper.addMixIn(CustomUserDetails.class, CustomUserDetailsMixin.class)} para permitir que
   * instancias de {@code CustomUserDetails} (que extienden clases de Spring Security) se conviertan
   * a JSON al persistir las autorizaciones y se reconstruyan al leerlas desde la base de datos.
   *
   * <p>Este mixin consigue lo siguiente:
   *
   * <ul>
   *   <li>Incluye información de tipo concreta durante la (de)serialización ({@link JsonTypeInfo}),
   *       necesaria para deserializar correctamente subclases y mantener la identificación de
   *       clase.
   *   <li>Forza la visibilidad de los campos privados para evitar depender exclusivamente de
   *       getters o de Lombok en tiempo de deserialización ({@link JsonAutoDetect}).
   *   <li>Ignora propiedades desconocidas para mantener compatibilidad hacia adelante ({@link
   *       JsonIgnoreProperties}).
   *   <li>Define un {@link JsonCreator} con las propiedades necesarias para recrear un {@code
   *       CustomUserDetails} (incluido el id de dominio y las autoridades).
   * </ul>
   *
   * <p>Es necesario porque las instancias de {@code CustomUserDetails} se almacenan en los
   * atributos/metadata de {@link
   * org.springframework.security.oauth2.server.authorization.OAuth2Authorization} y se serializan
   * con este {@link ObjectMapper} al persistir la entidad {@code Authorization}.
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  @JsonAutoDetect(
      fieldVisibility = JsonAutoDetect.Visibility.ANY,
      getterVisibility = JsonAutoDetect.Visibility.NONE,
      isGetterVisibility = JsonAutoDetect.Visibility.NONE)
  @JsonIgnoreProperties(ignoreUnknown = true)
  private abstract static class CustomUserDetailsMixin {

    @JsonCreator
    CustomUserDetailsMixin(
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("enabled") boolean enabled,
        @JsonProperty("accountNonExpired") boolean accountNonExpired,
        @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired,
        @JsonProperty("accountNonLocked") boolean accountNonLocked,
        @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {}
  }
}
