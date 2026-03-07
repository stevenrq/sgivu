package com.sgivu.purchasesale.exception;

/**
 * Se lanza cuando una entidad remota (cliente, vehículo o usuario) no se encuentra en ninguno de
 * los endpoints disponibles del microservicio correspondiente. Los servicios de clientes y
 * vehículos son polimórficos (persona/empresa, auto/motocicleta) y no exponen un endpoint unificado
 * de búsqueda, por lo que esta excepción indica que se agotaron todos los fallbacks posibles.
 */
public class EntityNotFoundException extends ContractBusinessException {

  public EntityNotFoundException(String message) {
    super(message);
  }

  public EntityNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
