from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional
import os

class Settings(BaseSettings):
    PROJECT_NAME: str = "Sync API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"

    # Database — Supabase PostgreSQL on prod, SQLite locally
    DATABASE_URL: str = os.getenv("DATABASE_URL", "sqlite:///./ghostmesh.db")

    # Supabase Storage (for profile photo uploads on Render)
    SUPABASE_URL: Optional[str] = os.getenv("SUPABASE_URL", None)
    SUPABASE_SERVICE_KEY: Optional[str] = os.getenv("SUPABASE_SERVICE_KEY", None)
    SUPABASE_BUCKET: str = os.getenv("SUPABASE_BUCKET", "avatars")

    # Proximity threshold in meters (e.g., within 40m = same room / building zone)
    PROXIMITY_RADIUS_METERS: float = 40.0

    # Maximum age for active location to be considered 'nearby' (in minutes)
    LOCATION_STALE_MINUTES: int = 10

    # Cooldown in hours between connection requests between the same 2 users
    CONNECTION_COOLDOWN_HOURS: int = 24

    model_config = SettingsConfigDict(case_sensitive=True, env_file=".env", extra="ignore")

settings = Settings()
