"""Router de health check."""

from fastapi import APIRouter

from app.infrastructure.config import get_settings

router = APIRouter(tags=["health"])
settings = get_settings()


@router.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "version": settings.app_version}
