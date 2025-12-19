package com.sgivu.auth.repository;

import com.sgivu.auth.entity.Authorization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio JPA para persistir autorizaciones y tokens emitidos por el Authorization Server.
 *
 * <p>Se almacena la negociación de consentimientos y tokens asociados a usuarios que operan el
 * inventario de vehículos usados, compras/ventas y contratos a través de clientes OIDC.
 */
public interface AuthorizationRepository extends JpaRepository<Authorization, String> {

  Optional<Authorization> findByState(String state);

  Optional<Authorization> findByAuthorizationCodeValue(String authorizationCode);

  Optional<Authorization> findByAccessTokenValue(String accessToken);

  Optional<Authorization> findByRefreshTokenValue(String refreshToken);

  Optional<Authorization> findByOidcIdTokenValue(String idToken);

  Optional<Authorization> findByUserCodeValue(String userCode);

  Optional<Authorization> findByDeviceCodeValue(String deviceCode);

  /**
   * Resuelve una autorización a partir de cualquier token emitido.
   *
   * <p>Permite reutilizar el mismo índice para revocaciones y validaciones en flujos distribuidos
   * (logout en Angular, revocación desde Postman, validación de refresh tokens).
   *
   * @param token valor de cualquier token (state, authorization_code, access_token, refresh_token,
   *     id_token, user_code, device_code).
   * @return autorización asociada si existe.
   */
  @Query(
      "select a from Authorization a where a.state = :token"
          + " or a.authorizationCodeValue = :token"
          + " or a.accessTokenValue = :token"
          + " or a.refreshTokenValue = :token"
          + " or a.oidcIdTokenValue = :token"
          + " or a.userCodeValue = :token"
          + " or a.deviceCodeValue = :token")
  Optional<Authorization>
      findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
          @Param("token") String token);
}
