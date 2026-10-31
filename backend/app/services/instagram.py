"""
Instagram profile verification service with high-speed 24-hour caching.
"""

import instaloader
import sqlite3
import time
from typing import Dict, Any
import httpx
from app.services.storage import is_supabase_configured, upload_photo_to_supabase

CACHE_DB = "/data/spott/spott.db"

def _init_cache_table():
    try:
        with sqlite3.connect(CACHE_DB) as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS instagram_cache (
                    username TEXT PRIMARY KEY,
                    full_name TEXT,
                    avatar_url TEXT,
                    description TEXT,
                    is_private INTEGER,
                    cached_at REAL
                )
            """)
            conn.commit()
    except Exception:
        pass

_init_cache_table()

_loader = instaloader.Instaloader(
    download_pictures=False,
    download_videos=False,
    download_video_thumbnails=False,
    download_geotags=False,
    download_comments=False,
    save_metadata=False,
    compress_json=False,
    quiet=True,
)

async def verify_instagram_profile(username: str) -> Dict[str, Any]:
    cleaned = username.strip().lstrip("@").lower()

    if not cleaned or len(cleaned) < 1:
        return {
            "exists": False,
            "username": cleaned,
            "error": "Username cannot be empty"
        }

    # 1. Check 24-hour cache first (instant <5ms response)
    try:
        with sqlite3.connect(CACHE_DB) as conn:
            cur = conn.cursor()
            cur.execute("SELECT full_name, avatar_url, description, is_private, cached_at FROM instagram_cache WHERE username = ?", (cleaned,))
            row = cur.fetchone()
            if row and (time.time() - row[4]) < 86400: # 24 hours
                return {
                    "exists": True,
                    "username": cleaned,
                    "full_name": row[0] or cleaned,
                    "avatar_url": row[1] or "",
                    "description": row[2] or "",
                    "is_private": bool(row[3]),
                    "error": None
                }
    except Exception:
        pass

    # 2. Query Instagram directly using instaloader
    try:
        profile = instaloader.Profile.from_username(_loader.context, cleaned)
        avatar_url = profile.profile_pic_url or ""

        if avatar_url and is_supabase_configured():
            try:
                async with httpx.AsyncClient(timeout=10.0) as client:
                    pic_res = await client.get(avatar_url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
                    if pic_res.status_code == 200:
                        avatar_url = await upload_photo_to_supabase(pic_res.content, f"{cleaned}.jpg")
            except Exception:
                pass

        result = {
            "exists": True,
            "username": profile.username,
            "full_name": profile.full_name or cleaned,
            "avatar_url": avatar_url,
            "description": profile.biography or "",
            "is_private": profile.is_private,
            "error": None
        }

        # Cache result
        try:
            with sqlite3.connect(CACHE_DB) as conn:
                conn.execute(
                    "INSERT OR REPLACE INTO instagram_cache (username, full_name, avatar_url, description, is_private, cached_at) VALUES (?, ?, ?, ?, ?, ?)",
                    (cleaned, result["full_name"], result["avatar_url"], result["description"], int(result["is_private"]), time.time())
                )
                conn.commit()
        except Exception:
            pass

        return result

    except instaloader.exceptions.ProfileNotExistsException:
        return {"exists": False, "username": cleaned, "error": f"Instagram account '@{cleaned}' does not exist"}
    except Exception as e:
        # 3. Fallback to Render service if local instaloader hit rate limit
        try:
            async with httpx.AsyncClient(timeout=8.0) as client:
                res = await client.get(f"https://sync-enoc.onrender.com/api/v1/users/verify-instagram/{cleaned}")
                if res.status_code == 200:
                    data = res.json()
                    if data.get("exists"):
                        try:
                            with sqlite3.connect(CACHE_DB) as conn:
                                conn.execute(
                                    "INSERT OR REPLACE INTO instagram_cache (username, full_name, avatar_url, description, is_private, cached_at) VALUES (?, ?, ?, ?, ?, ?)",
                                    (cleaned, data.get("full_name"), data.get("avatar_url"), data.get("description"), int(data.get("is_private", False)), time.time())
                                )
                                conn.commit()
                        except Exception:
                            pass
                        return data
        except Exception:
            pass

        return {"exists": False, "username": cleaned, "error": f"Could not verify Instagram profile: {str(e)}"}
