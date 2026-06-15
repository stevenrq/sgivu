package com.sgivu.vehicle.exception;

/**
 * Excepción personalizada para indicar que el servicio de almacenamiento no está disponible. Esta
 * excepción se lanza cuando hay un error de conexión o un problema con el servicio S3
 */
public class StorageUnavailableException extends RuntimeException {

  public StorageUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
