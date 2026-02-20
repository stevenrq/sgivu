package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.client.ClientServiceClient;
import com.sgivu.purchasesale.client.UserServiceClient;
import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.exception.ContractValidationException;
import com.sgivu.purchasesale.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Servicio encargado de verificar la existencia de entidades externas (clientes, vehículos y
 * usuarios) consumiendo los microservicios correspondientes. Los servicios de clientes y vehículos
 * son polimórficos: un cliente puede ser persona o empresa, y un vehículo puede ser auto o
 * motocicleta. Como estos microservicios no exponen un endpoint unificado de búsqueda por ID, se
 * emplea un patrón de fallback secuencial (try persona → catch 404 → try empresa) para resolver la
 * entidad sin conocer su subtipo a priori.
 */
@Service
public class EntityResolutionService {

  private final ClientServiceClient clientServiceClient;
  private final VehicleServiceClient vehicleServiceClient;
  private final UserServiceClient userServiceClient;

  public EntityResolutionService(
      ClientServiceClient clientServiceClient,
      VehicleServiceClient vehicleServiceClient,
      UserServiceClient userServiceClient) {
    this.clientServiceClient = clientServiceClient;
    this.vehicleServiceClient = vehicleServiceClient;
    this.userServiceClient = userServiceClient;
  }

  /**
   * Resuelve el ID de un cliente verificando su existencia en los endpoints de persona y empresa.
   * Se intenta primero persona porque es el tipo de cliente más frecuente en el sistema.
   *
   * @param clientId ID del cliente a resolver
   * @return el ID confirmado del cliente
   * @throws ContractValidationException si el ID es nulo
   * @throws EntityNotFoundException si no se encuentra como persona ni como empresa
   * @throws HttpClientErrorException si ocurre un error HTTP distinto a 404
   */
  public Long resolveClientId(Long clientId) {
    if (clientId == null) {
      throw new ContractValidationException("El ID del cliente debe ser proporcionado.");
    }

    // Intentar como persona primero (tipo más frecuente)
    try {
      return clientServiceClient.getPersonById(clientId).getId();
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().value() != 404) {
        throw exception;
      }
    }

    // Fallback a empresa cuando persona retorna 404
    try {
      return clientServiceClient.getCompanyById(clientId).getId();
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().value() == 404) {
        throw new EntityNotFoundException("Cliente no encontrado con id: " + clientId, exception);
      }
      throw exception;
    }
  }

  /**
   * Resuelve el ID de un vehículo verificando su existencia en los endpoints de auto y motocicleta.
   * Se intenta primero auto porque representa la mayoría de vehículos en inventario.
   *
   * @param vehicleId ID del vehículo a resolver
   * @return el ID confirmado del vehículo
   * @throws ContractValidationException si el ID es nulo
   * @throws EntityNotFoundException si no se encuentra como auto ni como motocicleta
   * @throws HttpClientErrorException si ocurre un error HTTP distinto a 404
   */
  public Long resolveVehicleId(Long vehicleId) {
    if (vehicleId == null) {
      throw new ContractValidationException("El ID del vehículo debe ser proporcionado.");
    }

    // Intentar como auto primero (tipo más frecuente)
    try {
      return vehicleServiceClient.getCarById(vehicleId).getId();
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().value() != 404) {
        throw exception;
      }
    }

    // Fallback a motocicleta cuando auto retorna 404
    try {
      return vehicleServiceClient.getMotorcycleById(vehicleId).getId();
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().value() == 404) {
        throw new EntityNotFoundException("Vehículo no encontrado con id: " + vehicleId, exception);
      }
      throw exception;
    }
  }

  /**
   * Resuelve el ID de un usuario verificando su existencia en el microservicio de usuarios.
   *
   * @param userId ID del usuario a resolver
   * @return el ID confirmado del usuario
   * @throws ContractValidationException si el ID es nulo
   */
  public Long resolveUserId(Long userId) {
    if (userId == null) {
      throw new ContractValidationException("El ID del usuario debe ser proporcionado.");
    }
    return userServiceClient.getUserById(userId).getId();
  }
}
