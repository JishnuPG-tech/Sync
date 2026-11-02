import os
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from typing import Dict, Set
from app.config import settings
from app.routers import users, radar, connections

app = FastAPI(
    title="Spott API",
    version=settings.VERSION,
    description="Privacy-first proximity discovery API — Spott (High-Speed Cloud Engine).",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS — allow Android APK, web clients, previews
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

# Real-Time WebSocket Connection Manager for Live Radar Streaming
class RadarStreamManager:
    def __init__(self):
        self.active_streams: Dict[str, Set[WebSocket]] = {}

    async def connect(self, user_id: str, websocket: WebSocket):
        await websocket.accept()
        if user_id not in self.active_streams:
            self.active_streams[user_id] = set()
        self.active_streams[user_id].add(websocket)

    def disconnect(self, user_id: str, websocket: WebSocket):
        if user_id in self.active_streams:
            self.active_streams[user_id].discard(websocket)
            if not self.active_streams[user_id]:
                del self.active_streams[user_id]

    async def broadcast_nearby_event(self, target_user_id: str, event_data: dict):
        if target_user_id in self.active_streams:
            for ws in list(self.active_streams[target_user_id]):
                try:
                    await ws.send_json(event_data)
                except Exception:
                    pass

radar_stream_manager = RadarStreamManager()

@app.websocket("/api/v1/radar/stream/{user_id}")
async def websocket_radar_endpoint(websocket: WebSocket, user_id: str):
    await radar_stream_manager.connect(user_id, websocket)
    try:
        while True:
            data = await websocket.receive_text()
            if data == "ping":
                await websocket.send_text("pong")
    except WebSocketDisconnect:
        radar_stream_manager.disconnect(user_id, websocket)

@app.get("/", tags=["Root"])
def root():
    return {
        "status": "online",
        "service": "Spott API",
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/health", tags=["Health"])
def health_check():
    return {
        "status": "healthy",
        "service": "Spott API",
        "version": settings.VERSION,
        "engine": "Hermes Cloud Xeon Platinum",
        "database": "sqlite_wal_fast",
        "uptime": "24/7"
    }

from fastapi.responses import FileResponse

@app.get("/api/v1/promo-video", tags=["Promo"])
def get_promo_video():
    return FileResponse("/data/antigravity/spott_promo/brag_spott.mp4", media_type="video/mp4", filename="Spott-promo.mp4")

@app.get("/api/v1/promo-poster", tags=["Promo"])
def get_promo_poster():
    return FileResponse("/data/antigravity/spott_promo/brag.jpg", media_type="image/jpeg")
