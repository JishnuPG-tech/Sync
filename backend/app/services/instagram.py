"""
High-Speed Instagram profile verification service for Spott.
Uses Render mirror & Instaloader, uploads JPEG to Supabase Storage,
and returns native PNG/JPEG images decoded seamlessly by Android BitmapFactory.
Never locks users into DiceBear identicons if a real Instagram photo exists.
"""

import asyncio
import instaloader
from typing import Dict, Any, Optional
import httpx
from app.services.storage import is_supabase_configured, upload_photo_to_supabase
from app.supabase_client import get_supabase
from app import crud

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

def is_real_avatar(url: Optional[str]) -> bool:
    """Returns True only if url points to a real photograph, not an identicon or SVG."""
    if not url:
        return False
    u = url.lower()
    if u.endswith(".svg"):
        return False
    if "dicebear.com" in u:
        return False
    return "http://" in u or "https://" in u

async def verify_instagram_profile(username: str) -> Dict[str, Any]:
    cleaned = username.strip().lstrip("@").lower()

    if not cleaned or len(cleaned) < 1:
        return {
            "exists": False,
            "username": cleaned,
            "error": "Username cannot be empty"
        }

    # 1. Check local/Supabase cache first (instant sub-millisecond response)
    # ONLY if cached user has a verified real photograph (never cache DiceBear fallbacks)
    cached_user = crud.get_user_by_instagram(cleaned)
    if cached_user and is_real_avatar(cached_user.get("avatar_url")):
        return {
            "exists": True,
            "username": cleaned,
            "full_name": cached_user.get("display_name") or cleaned,
            "avatar_url": cached_user.get("avatar_url"),
            "description": cached_user.get("bio") or "",
            "is_private": False,
            "error": None
        }

    # 2. Fast Mirror: Query Render service directly (returns Supabase Storage CDN JPG)
    try:
        async with httpx.AsyncClient(timeout=9.0) as client:
            res = await client.get(f"https://sync-enoc.onrender.com/api/v1/users/verify-instagram/{cleaned}")
            if res.status_code == 200:
                data = res.json()
                if data.get("exists") and is_real_avatar(data.get("avatar_url")):
                    # Auto-upgrade local cache if user exists
                    if cached_user and not is_real_avatar(cached_user.get("avatar_url")):
                        try:
                            with crud.sqlite3.connect(crud.DB_PATH) as conn:
                                conn.execute("UPDATE users SET avatar_url = ?, display_name = ? WHERE instagram_username = ?",
                                             (data["avatar_url"], data.get("full_name") or cleaned, cleaned))
                                conn.commit()
                        except Exception:
                            pass
                    return data
    except Exception:
        pass

    # 3. Direct Instaloader query with 4.0s timeout
    try:
        def _fetch_profile():
            return instaloader.Profile.from_username(_loader.context, cleaned)

        profile = await asyncio.wait_for(asyncio.to_thread(_fetch_profile), timeout=4.0)
        avatar_url = profile.profile_pic_url or ""

        if avatar_url and is_supabase_configured():
            try:
                async with httpx.AsyncClient(timeout=5.0) as client:
                    pic_res = await client.get(avatar_url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
                    if pic_res.status_code == 200:
                        avatar_url = await upload_photo_to_supabase(pic_res.content, f"{cleaned}.jpg")
            except Exception:
                pass

        if avatar_url and is_real_avatar(avatar_url):
            return {
                "exists": True,
                "username": profile.username,
                "full_name": profile.full_name or cleaned,
                "avatar_url": avatar_url,
                "description": profile.biography or "",
                "is_private": profile.is_private,
                "error": None
            }
    except instaloader.exceptions.ProfileNotExistsException:
        return {
            "exists": False,
            "username": cleaned,
            "error": f"Instagram account @{cleaned} does not exist"
        }
    except Exception:
        pass

    # 4. Graceful PNG Fallback (Never return SVG because Android BitmapFactory cannot decode SVG)
    return {
        "exists": True,
        "username": cleaned,
        "full_name": cleaned,
        "avatar_url": f"https://api.dicebear.com/7.x/identicon/png?seed={cleaned}",
        "description": "",
        "is_private": False,
        "error": None
    }
