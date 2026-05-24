package com.sgivu.auth.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.security.Principal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Controlador para renderizar la pantalla de consentimiento personalizada. */
@Hidden
@Controller
public class AuthorizationConsentController {

  private static final String AUTHORIZATION_ENDPOINT_URI = "/oauth2/authorize";
  private static final String DEFAULT_SCOPE_TITLE = "Permiso de acceso";
  private static final String DEFAULT_SCOPE_DESCRIPTION =
      "Este permiso es requerido por la aplicación para completar la autenticación solicitada.";
  private static final Map<String, ScopeMetadata> SCOPE_METADATA = createScopeMetadata();

  private final RegisteredClientRepository registeredClientRepository;
  private final OAuth2AuthorizationConsentService authorizationConsentService;

  public AuthorizationConsentController(
      RegisteredClientRepository registeredClientRepository,
      OAuth2AuthorizationConsentService authorizationConsentService) {
    this.registeredClientRepository = registeredClientRepository;
    this.authorizationConsentService = authorizationConsentService;
  }

  @GetMapping("/oauth2/consent")
  public String consent(
      Principal principal,
      Model model,
      @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
      @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
      @RequestParam(OAuth2ParameterNames.STATE) String state) {

    RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);
    if (registeredClient == null) {
      throw new IllegalArgumentException("Registered client not found.");
    }

    OAuth2AuthorizationConsent currentAuthorizationConsent =
        this.authorizationConsentService.findById(registeredClient.getId(), principal.getName());

    Set<String> authorizedScopes =
        currentAuthorizationConsent != null
            ? currentAuthorizationConsent.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toCollection(LinkedHashSet::new))
            : Collections.emptySet();

    Set<ScopeView> scopesToApprove = new LinkedHashSet<>();
    Set<ScopeView> previouslyApprovedScopes = new LinkedHashSet<>();

    for (String requestedScope : StringUtils.delimitedListToStringArray(scope, " ")) {
      if (OidcScopes.OPENID.equals(requestedScope)) {
        continue;
      }

      ScopeView scopeView = ScopeView.from(requestedScope);
      if (authorizedScopes.contains(requestedScope)) {
        previouslyApprovedScopes.add(scopeView);
      } else {
        scopesToApprove.add(scopeView);
      }
    }

    String clientDisplayName =
        StringUtils.hasText(registeredClient.getClientName())
            ? registeredClient.getClientName()
            : registeredClient.getClientId();

    model.addAttribute("clientId", clientId);
    model.addAttribute("clientDisplayName", clientDisplayName);
    model.addAttribute("state", state);
    model.addAttribute("principalName", principal.getName());
    model.addAttribute("requestUri", AUTHORIZATION_ENDPOINT_URI);
    model.addAttribute("scopes", scopesToApprove);
    model.addAttribute("previouslyApprovedScopes", previouslyApprovedScopes);
    return "oauth2-consent";
  }

  private static Map<String, ScopeMetadata> createScopeMetadata() {
    Map<String, ScopeMetadata> metadata = new LinkedHashMap<>();
    metadata.put(
        OidcScopes.PROFILE,
        new ScopeMetadata(
            "Perfil básico",
            "Permite consultar información básica de tu perfil para personalizar la sesión."));
    metadata.put(
        OidcScopes.EMAIL,
        new ScopeMetadata(
            "Correo electrónico",
            "Permite conocer tu correo registrado para identificar tu cuenta dentro de SGIVU."));
    metadata.put(
        OidcScopes.PHONE,
        new ScopeMetadata(
            "Teléfono",
            "Permite acceder a tu número telefónico asociado para completar tu perfil."));
    metadata.put(
        OidcScopes.ADDRESS,
        new ScopeMetadata(
            "Dirección",
            "Permite consultar tu dirección registrada cuando sea necesaria para operaciones del"
                + " sistema."));
    metadata.put(
        "offline_access",
        new ScopeMetadata(
            "Acceso continuo",
            "Permite mantener tu sesión activa de forma segura sin pedir autenticación en cada"
                + " renovación."));
    metadata.put(
        "api",
        new ScopeMetadata(
            "Acceso a la API",
            "Autoriza a la aplicación a invocar los servicios protegidos necesarios para operar"
                + " SGIVU."));
    metadata.put(
        "read",
        new ScopeMetadata(
            "Lectura de información",
            "Permite consultar información autorizada dentro de los módulos disponibles para tu"
                + " usuario."));
    metadata.put(
        "write",
        new ScopeMetadata(
            "Actualización de información",
            "Permite crear o modificar datos según los permisos asignados a tu cuenta."));
    return Collections.unmodifiableMap(metadata);
  }

  private record ScopeMetadata(String title, String description) {}

  public record ScopeView(String scope, String title, String description) {

    static ScopeView from(String scope) {
      ScopeMetadata metadata =
          SCOPE_METADATA.getOrDefault(
              scope, new ScopeMetadata(DEFAULT_SCOPE_TITLE, DEFAULT_SCOPE_DESCRIPTION));
      return new ScopeView(scope, metadata.title(), metadata.description());
    }
  }
}
