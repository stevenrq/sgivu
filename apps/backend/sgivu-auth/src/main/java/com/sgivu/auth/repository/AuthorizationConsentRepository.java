package com.sgivu.auth.repository;

import com.sgivu.auth.entity.AuthorizationConsent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para persistir los consentimientos otorgados por los usuarios a cada cliente
 * OIDC.
 *
 * <p>Garantiza trazabilidad de qué scopes del inventario/ventas se aprobaron para cada sesión y
 * permite revocar accesos cuando un vehículo usado cambia de estado o se migran contratos.
 */
public interface AuthorizationConsentRepository
    extends JpaRepository<AuthorizationConsent, AuthorizationConsent.AuthorizationConsentId> {

  /**
   * Obtiene el consentimiento otorgado a un cliente específico por un principal.
   *
   * @param registeredClientId identificador del cliente registrado.
   * @param principalName nombre del usuario autenticado.
   * @return consentimiento existente o vacío si el usuario no ha otorgado permisos.
   */
  Optional<AuthorizationConsent> findByRegisteredClientIdAndPrincipalName(
      String registeredClientId, String principalName);

  /**
   * Revoca los consentimientos previamente registrados para un cliente y usuario determinados.
   *
   * @param registeredClientId identificador del cliente.
   * @param principalName principal (username) que otorgó el consentimiento.
   */
  void deleteByRegisteredClientIdAndPrincipalName(String registeredClientId, String principalName);
}
