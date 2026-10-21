import base64
import os
import uuid
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas import (
    UserCreate,
    UserPrivateProfile,
    UserUpdate,
    InstagramVerificationResponse,
    PhotoUploadPayload,
    PhotoUploadResponse
)
from app.services.instagram import verify_instagram_profile
from app.services.storage import upload_photo_to_supabase, is_supabase_configured
from app import crud

router = APIRouter(prefix="/users", tags=["Users"])

# Local fallback upload dir (used when running locally without Supabase env vars)
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "static", "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)


@router.post("/upload-photo", response_model=PhotoUploadResponse)
async def upload_profile_photo(payload: PhotoUploadPayload):
    """
    Saves a Base64-encoded profile photo.
    - If SUPABASE_URL + SUPABASE_SERVICE_KEY are set: uploads to Supabase Storage → returns CDN URL.
    - Otherwise (local dev): saves to local /static/uploads/ → returns relative path.
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
            # Cloud path: Supabase Storage
            photo_url = await upload_photo_to_supabase(image_data, filename)
        else:
            # Local dev path: save to disk
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
    """
    Multipart file upload alternative for profile photo.
    Uploads to Supabase Storage if configured, otherwise saves locally.
    """
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
    """
    Checks if an Instagram username exists and returns its public profile preview
    (avatar, full name, bio) so the user can visually confirm it is theirs.
    """
    result = await verify_instagram_profile(username)
    return InstagramVerificationResponse(**result)


@router.post("/register", response_model=UserPrivateProfile, status_code=status.HTTP_201_CREATED)
def register_user(user_in: UserCreate, db: Session = Depends(get_db)):
    """Register a new user with their display name and private Instagram handle."""
    existing = crud.get_user_by_instagram(db, user_in.instagram_username)
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Instagram handle is already registered"
        )
    user = crud.create_user(db, user_in)
    return _to_profile(user)


@router.get("/{user_id}", response_model=UserPrivateProfile)
def get_user_profile(user_id: str, db: Session = Depends(get_db)):
    """Get the authenticated user's own profile."""
    user = crud.get_user_by_id(db, user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    return _to_profile(user)


@router.patch("/{user_id}/discovery", response_model=UserPrivateProfile)
def toggle_discovery(user_id: str, is_active: bool, db: Session = Depends(get_db)):
    """Toggle Ghost Mode / Radar discovery ON or OFF."""
    user = crud.update_user_discovery(db, user_id, is_active)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    return _to_profile(user)


def _to_profile(user) -> UserPrivateProfile:
    """Helper to convert a User ORM object to the UserPrivateProfile schema."""
    return UserPrivateProfile(
        id=user.id,
        display_name=user.display_name,
        sync_username=user.sync_username,
        instagram_username=user.instagram_username,
        bio=user.bio or "",
        location=user.location_name or "",
        avatar_url=user.avatar_url,
        ble_token=user.ble_token,
        interests=user.interests_list,
        is_discovery_active=user.is_discovery_active,
        created_at=user.created_at
    )
