import base64
import os
import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, HTTPException, status, UploadFile, File
from app.schemas import (
    UserCreate,
    UserPrivateProfile,
    InstagramVerificationResponse,
    PhotoUploadPayload,
    PhotoUploadResponse
)
from app.services.instagram import verify_instagram_profile
from app.services.storage import upload_photo_to_supabase, is_supabase_configured
from app.supabase_client import get_supabase
from app import crud

router = APIRouter(prefix="/users", tags=["Users"])

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "static", "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)


@router.post("/upload-photo", response_model=PhotoUploadResponse)
async def upload_profile_photo(payload: PhotoUploadPayload):
    """
    Saves a Base64-encoded profile photo.
    - If Supabase Storage is configured: uploads to Supabase Storage -> returns CDN URL.
    - Otherwise (local dev): saves to local disk -> returns relative path.
    """
    try:
        raw_b64 = payload.image_base64
        if "," in raw_b64:
            raw_b64 = raw_b64.split(",")[1]
        image_data = base64.b64decode(raw_b64)

        file_ext = ".jpg"
        if payload.filename and "." in payload.filename:
            ext = os.path.splitext(payload.filename)[1].lower()
            if ext in [".jpg", ".jpeg", ".png", ".webp"]:
                file_ext = ext

        filename = f"photo{file_ext}"

        if is_supabase_configured():
            photo_url = await upload_photo_to_supabase(image_data, filename)
        else:
            unique_name = f"avatar_{uuid.uuid4().hex[:12]}{file_ext}"
            file_path = os.path.join(UPLOAD_DIR, unique_name)
            with open(file_path, "wb") as f:
                f.write(image_data)
            photo_url = f"/static/uploads/{unique_name}"

        return PhotoUploadResponse(photo_url=photo_url, success=True)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to process image: {str(e)}")


@router.post("/upload-photo-file")
async def upload_photo_file(file: UploadFile = File(...)):
    """Multipart file upload alternative."""
    try:
        ext = os.path.splitext(file.filename)[1].lower() if file.filename else ".jpg"
        contents = await file.read()

        if is_supabase_configured():
            photo_url = await upload_photo_to_supabase(contents, f"photo{ext}")
        else:
            unique_name = f"avatar_{uuid.uuid4().hex[:12]}{ext}"
            file_path = os.path.join(UPLOAD_DIR, unique_name)
            with open(file_path, "wb") as f:
                f.write(contents)
            photo_url = f"/static/uploads/{unique_name}"

        return {"photo_url": photo_url, "success": True}
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to save file: {str(e)}")


@router.get("/verify-instagram/{username}", response_model=InstagramVerificationResponse)
async def verify_instagram_handle(username: str):
    """Verifies that an Instagram account exists and returns its public profile."""
    result = await verify_instagram_profile(username)
    return InstagramVerificationResponse(**result)


@router.post("/register", response_model=UserPrivateProfile, status_code=status.HTTP_201_CREATED)
def register_user(user_in: UserCreate):
    """Registers user or logs in existing user directly in Supabase."""
    existing = crud.get_user_by_instagram(user_in.instagram_username)
    if existing:
        # Existing user logging back in: update profile fields and ensure discovery is active
        supabase = get_supabase()
        now_iso = datetime.now(timezone.utc).isoformat()
        update_data = {
            "display_name": user_in.display_name.strip(),
            "sync_username": user_in.sync_username.strip().lower() if user_in.sync_username else existing.get("sync_username"),
            "bio": user_in.bio.strip() if user_in.bio else "",
            "location_name": user_in.location.strip() if user_in.location else "",
            "avatar_url": user_in.avatar_url,
            "is_discovery_active": True,
            "updated_at": now_iso
        }
        res = supabase.table("users").update(update_data).eq("id", existing["id"]).execute()
        updated = res.data[0] if res.data else existing
        return _dict_to_profile(updated)

    user = crud.create_user(user_in)
    return _dict_to_profile(user)


@router.get("/{user_id}", response_model=UserPrivateProfile)
def get_user_profile(user_id: str):
    """Fetches user private profile by ID."""
    user = crud.get_user_by_id(user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    return _dict_to_profile(user)


@router.patch("/{user_id}/discovery", response_model=UserPrivateProfile)
def toggle_discovery(user_id: str, is_active: bool):
    """Toggles Ghost Mode / Discovery ON or OFF."""
    user = crud.update_user_discovery(user_id, is_active)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    return _dict_to_profile(user)


def _dict_to_profile(user: dict) -> UserPrivateProfile:
    interests = [i.strip() for i in user.get("interests_csv", "").split(",") if i.strip()]
    created_at = None
    if user.get("created_at"):
        try:
            created_at = datetime.fromisoformat(user["created_at"])
        except Exception:
            created_at = datetime.now(timezone.utc)

    return UserPrivateProfile(
        id=user["id"],
        display_name=user["display_name"],
        sync_username=user.get("sync_username"),
        instagram_username=user["instagram_username"],
        bio=user.get("bio", ""),
        location=user.get("location_name", ""),
        avatar_url=user.get("avatar_url"),
        ble_token=user.get("ble_token", ""),
        interests=interests,
        is_discovery_active=user.get("is_discovery_active", True),
        created_at=created_at or datetime.now(timezone.utc)
    )
