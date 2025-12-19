package com.sgivu.auth.repository;

import com.sgivu.auth.entity.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para persistir clientes OAuth2 registrados en el Authorization Server SGIVU.
 *
 * <p>Contiene metadata de los canales de compra/venta (Angular, Postman, servicios internos) que
 * consumen tokens para operar sobre inventario, contratos o predicción de demanda.
 */
public interface ClientRepository extends JpaRepository<Client, String> {

  /**
   * Busca un cliente por su identificador público configurado en flujos OAuth2/OIDC.
   *
   * @param clientId identificador del cliente (ej. {@code angular-client}).
   * @return cliente persistido o vacío si no existe.
   */
  Optional<Client> findByClientId(String clientId);
}
