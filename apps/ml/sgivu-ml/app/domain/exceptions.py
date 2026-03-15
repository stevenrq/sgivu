"""Excepciones de dominio (sin acoplamiento a frameworks HTTP).

Cada excepción representa una regla de negocio violada.  La capa API
se encarga de mapearlas a códigos HTTP apropiados.
"""


class DomainError(Exception):
    """Excepción base de dominio."""

    def __init__(self, message: str) -> None:
        self.message = message
        super().__init__(message)


class ModelNotTrainedError(DomainError):
    """No existe un modelo entrenado disponible."""


class InsufficientHistoryError(DomainError):
    """El historial de datos es insuficiente para la operación solicitada."""


class SegmentNotFoundError(DomainError):
    """No se encontró el segmento de vehículo solicitado en el historial."""


class DataLoadError(DomainError):
    """Error al cargar datos desde fuentes externas o loader no configurado."""


class MissingVehicleLineError(DomainError):
    """Falta la línea del vehículo en la solicitud."""


class TrainingError(DomainError):
    """Error durante el proceso de entrenamiento del modelo."""
