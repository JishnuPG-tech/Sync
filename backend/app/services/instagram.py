"""
Instagram profile verification service using instaloader.
This performs real, anonymous (no login required) Instagram profile lookups.
"""

import instaloader
from typing import Dict, Any

# Shared instaloader context (anonymous, no login)
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
    """
    Verify that an Instagram username exists and fetch their public profile info.
    Uses instaloader for anonymous (no-login) profile lookup.

    Returns:
        {
            exists: bool,
            username: str,
            full_name: str,       # Display name
            avatar_url: str,      # Profile picture CDN URL
            description: str,     # Biography / bio text
            is_private: bool,
            error: str | None
        }
    """
    cleaned = username.strip().lstrip("@").lower()

    if not cleaned or len(cleaned) < 1:
        return {
            "exists": False,
            "username": cleaned,
            "error": "Username cannot be empty"
        }

    try:
        profile = instaloader.Profile.from_username(_loader.context, cleaned)

        return {
            "exists": True,
            "username": profile.username,
            "full_name": profile.full_name or cleaned,
            "avatar_url": profile.profile_pic_url or "",
            "description": profile.biography or "",
            "is_private": profile.is_private,
            "error": None
        }

    except instaloader.exceptions.ProfileNotExistsException:
        return {
            "exists": False,
            "username": cleaned,
            "error": f"Instagram account '@{cleaned}' does not exist"
        }
    except instaloader.exceptions.LoginRequiredException:
        return {
            "exists": False,
            "username": cleaned,
            "error": f"Cannot verify '@{cleaned}' — profile is private or requires login"
        }
    except instaloader.exceptions.ConnectionException as e:
        return {
            "exists": False,
            "username": cleaned,
            "error": f"Connection error while verifying Instagram: {str(e)}"
        }
    except Exception as e:
        return {
            "exists": False,
            "username": cleaned,
            "error": f"Failed to verify Instagram profile: {str(e)}"
        }
