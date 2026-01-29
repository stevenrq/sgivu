from fastapi import FastAPI

from app.core.config import get_settings
from app.database.database import database_enabled, init_db
from app.routers import prediction_router

settings = get_settings()
app = FastAPI(title=settings.app_name, version=settings.app_version)


@app.on_event("startup")
async def startup() -> None:
    if settings.database_auto_create and database_enabled(settings):
        init_db(settings)


@app.get("/health", tags=["health"])
async def health():
    return {"status": "ok", "version": settings.app_version}


app.include_router(prediction_router.router)
