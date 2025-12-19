package com.sgivu.client.service;

import com.sgivu.client.entity.Client;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrato base para servicios de clientes (personas/empresas) que consumen los microservicios
 * SGIVU de inventario, contratos y predicción.
 *
 * @param <T> tipo concreto de cliente
 */
public interface ClientService<T extends Client> {

  /**
   * Persiste un cliente listo para ser utilizado en flujos de compra/venta.
   *
   * @param client entidad de cliente
   * @return entidad guardada
   */
  T save(T client);

  /**
   * Recupera un cliente por id.
   *
   * @param id identificador
   * @return cliente si existe
   */
  Optional<T> findById(Long id);

  /**
   * Lista completa de clientes.
   *
   * @return clientes
   */
  List<T> findAll();

  /**
   * Lista paginada de clientes para consumo batch.
   *
   * @param pageable configuración de paginación
   * @return página de clientes
   */
  Page<T> findAll(Pageable pageable);

  /**
   * Actualiza un cliente existente.
   *
   * @param id identificador
   * @param client datos entrantes
   * @return cliente actualizado si existe
   */
  Optional<T> update(Long id, T client);

  /**
   * Elimina un cliente por id.
   *
   * @param id identificador
   */
  void deleteById(Long id);

  /**
   * Busca cliente por correo para validar duplicados y contactos de contratos.
   *
   * @param email correo
   * @return cliente si existe
   */
  Optional<T> findByEmail(String email);

  /**
   * Cambia el estado habilitado para autorizar su uso en nuevos procesos.
   *
   * @param id identificador
   * @param isEnabled estado deseado
   * @return true si se actualizó
   */
  boolean changeStatus(Long id, boolean isEnabled);

  /**
   * Cuenta clientes por estado operativo.
   *
   * @param isEnabled estado
   * @return número de clientes
   */
  long countByIsEnabled(boolean isEnabled);
}
