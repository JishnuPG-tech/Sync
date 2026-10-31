#!/bin/bash
mkdir -p /data/spott /data/cache
export DATABASE_URL="sqlite:////data/spott/spott.db"
export PYTHONPATH="/data/antigravity/sync-app/backend:/app:"

fuser -k 8001/tcp 2>/dev/null || true

echo "Starting Spott FastAPI backend on 127.0.0.1:8001..."
exec /usr/local/bin/python3.11 -m uvicorn app.main:app --host 127.0.0.1 --port 8001 --workers 2
