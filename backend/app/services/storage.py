"""
Supabase Storage service for profile photo uploads.
On Render, the local filesystem is ephemeral — photos must be persisted externally.
When SUPABASE_URL / SUPABASE_SERVICE_KEY env vars are set, uploads go to Supabase Storage.
When running locally without those env vars, falls back to local disk.
"""

import base64
import os
import uuid
import httpx
from app.config import settings


async def upload_photo_to_supabase(image_bytes: bytes, filename: str) -> str:
    """
    Upload image bytes to Supabase Storage.
    Returns the public URL of the uploaded file.
    """
    bucket = settings.SUPABASE_BUCKET
    unique_name = f"avatar_{uuid.uuid4().hex[:16]}{os.path.splitext(filename)[1] or '.jpg'}"
    upload_url = f"{settings.SUPABASE_URL}/storage/v1/object/{bucket}/{unique_name}"

    headers = {
        "Authorization": f"Bearer {settings.SUPABASE_SERVICE_KEY}",
        "Content-Type": "image/jpeg",
        "x-upsert": "true",
    }

    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.post(upload_url, content=image_bytes, headers=headers)
        response.raise_for_status()

    # Return the public URL
    public_url = f"{settings.SUPABASE_URL}/storage/v1/object/public/{bucket}/{unique_name}"
    return public_url


def is_supabase_configured() -> bool:
    """Returns True if Supabase Storage env vars are set."""
    return bool(settings.SUPABASE_URL and settings.SUPABASE_SERVICE_KEY)
