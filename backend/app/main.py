import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from app.config import settings
from app.routers import users, radar, connections

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="Privacy-first proximity discovery API — Sync App (Powered by Supabase).",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS — allow Android APK, web clients, Render preview URLs
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Local upload fallback for offline development
static_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static")
if os.path.isdir(static_dir):
    os.makedirs(os.path.join(static_dir, "uploads"), exist_ok=True)
    app.mount("/static", StaticFiles(directory=static_dir), name="static")

# Mount API routers
app.include_router(users.router, prefix=settings.API_V1_STR)
app.include_router(radar.router, prefix=settings.API_V1_STR)
app.include_router(connections.router, prefix=settings.API_V1_STR)

@app.get("/", tags=["Root"])
def root():
    return {
        "status": "online",
        "service": settings.PROJECT_NAME,
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/health", tags=["Health"])
def health_check():
    return {
        "status": "healthy",
        "service": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "storage": "supabase" if settings.SUPABASE_URL else "local",
        "database": "supabase_rest_api"
    }
