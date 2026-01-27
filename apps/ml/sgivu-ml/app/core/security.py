from __future__ import annotations

import time
from typing import Any, Dict, Iterable, Set

import httpx
from authlib.jose import JoseError, JsonWebKey, JsonWebToken
from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .config import Settings, get_settings

bearer_scheme = HTTPBearer(auto_error=False)
jwt_backend = JsonWebToken(["RS256", "HS256"])


class JWKSCache:
    """Caché simple para llaves públicas JWKS."""

    def __init__(self) -> None:
        self.cached: Dict[str, Any] = {}
        self.expires_at: float = 0.0

    async def get_key_set(self, jwks_url: str, timeout: float) -> Any:
        if not jwks_url:
            return None

        now = time.time()
        if now >= self.expires_at:
            async with httpx.AsyncClient(timeout=timeout) as client:
                response = await client.get(jwks_url)
                response.raise_for_status()
                payload = response.json()
                self.cached = payload
                self.expires_at = now + 3600

        if not self.cached:
            return None
        return JsonWebKey.import_key_set(self.cached)


jwks_cache = JWKSCache()
oidc_cache: Dict[str, Any] = {}
oidc_expires_at: float = 0.0


async def discover_config(settings: Settings) -> dict | None:
    global oidc_cache, oidc_expires_at

    discovery_url = settings.sgivu_auth_discovery_url

    now = time.time()
    if oidc_cache and now < oidc_expires_at:
        return oidc_cache

    if not discovery_url:
        return None

    async with httpx.AsyncClient(timeout=settings.request_timeout_seconds) as client:
        response = await client.get(discovery_url)
        response.raise_for_status()
        payload = response.json()
        oidc_cache = payload
        oidc_expires_at = now + 3600
        return oidc_cache


async def decode_token(raw_token: str) -> dict:
    settings = get_settings()

    discovery_config = await discover_config(settings)
    jwks_url = (discovery_config or {}).get("jwks_uri")
    key_set: Any = None
    if jwks_url:
        key_set = await jwks_cache.get_key_set(
            jwks_url, settings.request_timeout_seconds
        )

    if not key_set:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="No hay llave publica configurada para validar el token.",
        )

    claims_options: Dict[str, Dict[str, Any]] = {
        "exp": {"essential": True},
        "iat": {"essential": True},
        "nbf": {"essential": False},
    }
    issuer = (discovery_config or {}).get("issuer")
    if issuer:
        claims_options["iss"] = {"values": [issuer]}

    try:
        claims = jwt_backend.decode(raw_token, key_set, claims_options=claims_options)
        claims.validate()
        return dict(claims)
    except JoseError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token invalido o expirado.",
        ) from exc


def _extract_permissions(claims: dict) -> Set[str]:
    raw = claims.get("rolesAndPermissions")
    if raw is None:
        return set()
    if isinstance(raw, str):
        # El issuer legacy entrega scopes separados por espacios o comas.
        parts = [
            token.strip() for token in raw.replace(",", " ").split() if token.strip()
        ]
        return set(parts)
    if isinstance(raw, (list, tuple, set)):
        return {str(item) for item in raw if item}
    return set()


async def require_token(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
) -> dict:
    if not credentials or not credentials.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Falta el encabezado Authorization.",
        )
    return await decode_token(credentials.credentials)


def require_permissions(required: Iterable[str]):
    required_set = {permission for permission in required if permission}

    def dependency(claims: dict = Depends(require_token)) -> dict:
        permissions = _extract_permissions(claims)
        if required_set and not permissions.intersection(required_set):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Falta permiso requerido para acceder al recurso.",
            )
        return claims

    return dependency


def require_internal_or_permissions(required: Iterable[str]):
    required_set = {permission for permission in required if permission}

    async def dependency(
        request: Request,
        credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    ) -> dict:
        settings = get_settings()
        internal_key = request.headers.get("X-Internal-Service-Key")

        if internal_key:
            if (
                settings.service_internal_secret_key
                and internal_key == settings.service_internal_secret_key
            ):
                # Inyecta solo los permisos mínimos para reutilizar el mismo flujo de autorización.
                return {"rolesAndPermissions": list(required_set)}
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Clave interna inválida.",
            )

        if not credentials or not credentials.credentials:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Falta el encabezado Authorization.",
            )

        claims = await decode_token(credentials.credentials)
        permissions = _extract_permissions(claims)
        if required_set and not permissions.intersection(required_set):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Falta permiso requerido para acceder al recurso.",
            )
        return claims

    return dependency
