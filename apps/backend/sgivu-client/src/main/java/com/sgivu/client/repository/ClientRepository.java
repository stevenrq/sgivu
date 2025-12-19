package com.sgivu.client.repository;

import com.sgivu.client.entity.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio base de clientes que habilita consultas y especificaciones JPA para personas y
 * empresas en SGIVU.
 *
 * @param <T> tipo concreto de cliente
 */
public interface ClientRepository<T extends Client>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

  /**
   * Busca un cliente por correo electrónico para validar duplicados o recuperar datos de contacto.
   *
   * @param email correo a consultar
   * @return cliente encontrado o vacío si no existe
   */
  Optional<T> findByEmail(String email);

  /**
   * Cuenta el número de clientes activos o inactivos según el estado proporcionado.
   *
   * @param isEnabled estado de activación (true para activos, false para inactivos)
   * @return número de clientes que coinciden con el estado proporcionado
   */
  long countByIsEnabled(boolean isEnabled);
}
