"""Caché de JWKS y descubrimiento OIDC.

Encapsula el estado de caché eliminando las variables globales mutables
que existían en el diseño anterior.
"""

from __future__ import annotations

import time
from typing import Any, Dict

import httpx
from authlib.jose import JsonWebKey

from app.infrastructure.config import Settings


class JWKSCache:
    """Caché simple para llaves públicas JWKS con expiración por TTL."""

    def __init__(self, ttl_seconds: float = 3600.0) -> None:
        self._cached: Dict[str, Any] = {}
        self._expires_at: float = 0.0
        self._ttl = ttl_seconds

    async def get_key_set(self, jwks_url: str, timeout: float) -> Any:
        """Obtiene el conjunto de llaves, refrescando la caché si expiró."""
        if not jwks_url:
            return None

        now = time.time()
        if now >= self._expires_at:
            async with httpx.AsyncClient(timeout=timeout) as client:
                response = await client.get(jwks_url)
                response.raise_for_status()
                payload = response.json()
                self._cached = payload
                self._expires_at = now + self._ttl

        if not self._cached:
            return None
        return JsonWebKey.import_key_set(self._cached)


class OIDCDiscoveryCache:
    """Caché para la configuración OIDC descubierta desde el issuer."""

    def __init__(self, ttl_seconds: float = 3600.0) -> None:
        self._config: Dict[str, Any] = {}
        self._expires_at: float = 0.0
        self._ttl = ttl_seconds

    async def discover(self, settings: Settings) -> dict[str, Any] | None:
        """Descubre y cachea la configuración OIDC del authorization server."""
        discovery_url = settings.sgivu_auth_discovery_url

        now = time.time()
        if self._config and now < self._expires_at:
            return self._config

        if not discovery_url:
            return None

        async with httpx.AsyncClient(
            timeout=settings.request_timeout_seconds
        ) as client:
            response = await client.get(discovery_url)
            response.raise_for_status()
            payload = response.json()
            self._config = payload
            self._expires_at = now + self._ttl
            return self._config


# Instancias singleton a nivel de módulo.
jwks_cache = JWKSCache()
oidc_discovery_cache = OIDCDiscoveryCache()
