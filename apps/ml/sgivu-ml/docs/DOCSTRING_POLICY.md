Política de Docstrings para sgivu-ml
==================================

Resumen
------

Usar docstrings en español con el estilo NumPy para todas las funciones,
clases y métodos públicos del servicio `sgivu-ml`. Los docstrings deben
documentar claramente: propósito, parámetros, valores de retorno, errores
esperados y notas relevantes sobre side-effects o coste computacional.

Formato
------

- Estilo: NumPy (compatible con `sphinxcontrib-napoleon`).
- Idioma: español (según convenciones del repo).
- Estructura mínima para funciones/métodos públicos:
  - Resumen (una línea).
  - Línea en blanco.
  - `Parameters` con tipo y descripción.
  - `Returns` con tipo y descripción.
  - `Raises` cuando aplique.
  - `Notes` o `Examples` opcional si ayuda a clarificar formatos.

Ejemplo
-------

def predict(filters: dict, horizon: int, confidence: float = 0.95) -> PredictionResult:
    """Predice demanda mensual para un segmento de vehículo.

    Parameters
    ----------
    filters : dict
        Diccionario con filtros (vehicle_type, brand, model, line).
    horizon : int
        Horizonte en meses.
    confidence : float, optional
        Nivel de confianza para intervalos (por defecto 0.95).

    Returns
    -------
    PredictionResult
        Estructura con predicciones, versión de modelo y métricas.

    Raises
    ------
    DataLoadError
        Si no hay origen de datos configurado.
    """

Herramientas y hooks
--------------------

- `pylint` y `black` para lint y formateo (configurados en `pyproject.toml`).
- Integrar en `pre-commit` y en CI para rechazar PRs sin docstrings
  en módulos críticos.

Buenas prácticas
----------------

- Documentar contratos JSON esperados al integrar con otros servicios.
- No incluir secretos en docstrings ni ejemplos reales de credenciales.
- Priorizar docstrings en: servicios de aplicación, orquestadores de ML,
  persistencia de artefactos y clientes HTTP.

Mantenimiento
------------

- El CI ya ejecuta pylint sobre `app/` y `tests/`; asegúrate de que los
  docstrings nuevos no introduzcan violaciones de convención.
- Revisar docstrings en cada cambio de API pública o cuando cambie la
  estructura de datos (columnas de DataFrame, formato de metadata, etc.).
