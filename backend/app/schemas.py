import re
from pydantic import BaseModel, Field, field_validator, ConfigDict
from typing import List, Optional
from datetime import datetime

# Official Instagram handle regex pattern
# 1-30 chars, letters, numbers, periods, underscores. No consecutive dots, cannot start/end with dot.
INSTAGRAM_HANDLE_REGEX = re.compile(r"^(?!.*\.\.)(?!^\.)[a-zA-Z0-9._]{1,30}(?<!\.)$")

class UserBase(BaseModel):
    display_name: str = Field(..., min_length=2, max_length=50, description="Display name, e.g. Arun K")
    sync_username: Optional[str] = Field(None, max_length=30, description="Sync handle, e.g. arun_k")
    bio: Optional[str] = Field(default="", max_length=160, description="Short introduction / bio")
    location: Optional[str] = Field(default="", max_length=100, description="Location or campus name")
    avatar_url: Optional[str] = Field(default=None, description="Profile photo URL or relative path")
    interests: List[str] = Field(default=[], description="List of interest tags, e.g. ['Tech', 'Fitness']")

    @field_validator("display_name")
    @classmethod
    def validate_display_name(cls, v: str) -> str:
        cleaned = v.strip()
        if len(cleaned) < 2:
            raise ValueError("Display name must be at least 2 characters long")
        if len(cleaned) > 50:
            raise ValueError("Display name cannot exceed 50 characters")
        return cleaned

class UserCreate(UserBase):
    instagram_username: str = Field(..., min_length=1, max_length=30, description="Private Instagram handle")
    fcm_token: Optional[str] = Field(None, description="FCM device push token")

    @field_validator("instagram_username")
    @classmethod
    def validate_instagram_handle(cls, v: str) -> str:
        # Strip leading '@' if user typed it
        cleaned = v.strip().lstrip("@")
        if not cleaned:
            raise ValueError("Instagram username cannot be empty")
        
        if " " in cleaned:
            raise ValueError("Instagram username cannot contain spaces")
            
        if not INSTAGRAM_HANDLE_REGEX.match(cleaned):
            if cleaned.startswith("."):
                raise ValueError("Instagram username cannot start with a period (.)")
            elif cleaned.endswith("."):
                raise ValueError("Instagram username cannot end with a period (.)")
            elif ".." in cleaned:
                raise ValueError("Instagram username cannot contain consecutive periods (..)")
            else:
                raise ValueError("Instagram username can only contain letters, numbers, periods (.), and underscores (_)")
                
        return cleaned.lower()

class UserUpdate(BaseModel):
    display_name: Optional[str] = None
    sync_username: Optional[str] = None
    bio: Optional[str] = None
    location: Optional[str] = None
    avatar_url: Optional[str] = None
    interests: Optional[List[str]] = None
    is_discovery_active: Optional[bool] = None
    fcm_token: Optional[str] = None

class UserPrivateProfile(UserBase):
    """Returned ONLY to the authenticated account owner."""
    id: str
    instagram_username: str
    ble_token: Optional[str] = None
    is_discovery_active: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class NearbyUserCard(BaseModel):
    """
    CRITICAL PRIVACY SHIELD:
    Returned to nearby users on radar.
    Instagram username is STRICTLY EXCLUDED!
    """
    user_id: str
    display_name: str
    bio: Optional[str] = ""
    location: Optional[str] = ""
    avatar_url: Optional[str] = None
    interests: List[str]
    approx_distance_meters: float
    connection_status: str = "NONE" # "NONE", "PENDING_SENT", "PENDING_RECEIVED", "CONNECTED"

class LocationPing(BaseModel):
    user_id: str
    latitude: float = Field(..., ge=-90.0, le=90.0)
    longitude: float = Field(..., ge=-180.0, le=180.0)

class ConnectionRequestCreate(BaseModel):
    sender_id: str
    receiver_id: str

class ConnectionRequestOut(BaseModel):
    """Sent to receiver when a nearby user requests connection."""
    id: str
    sender_id: str
    sender_display_name: str
    sender_bio: Optional[str] = ""
    sender_location: Optional[str] = ""
    sender_avatar_url: Optional[str] = None
    sender_interests: List[str]
    status: str
    created_at: datetime

class ConnectionRespond(BaseModel):
    action: str = Field(..., pattern="^(ACCEPT|DECLINE)$")

class MutualConnectionReveal(BaseModel):
    """
    REVEAL CARD:
    Unlocked ONLY after BOTH users have accepted the connection!
    """
    connection_id: str
    connected_user_id: str
    display_name: str
    instagram_username: str
    bio: Optional[str] = ""
    location: Optional[str] = ""
    avatar_url: Optional[str] = None
    interests: List[str]
    connected_at: Optional[datetime] = None

class PhotoUploadPayload(BaseModel):
    image_base64: str
    filename: Optional[str] = "profile.jpg"

class PhotoUploadResponse(BaseModel):
    photo_url: str
    success: bool = True

class InstagramVerificationResponse(BaseModel):
    exists: bool
    username: str
    full_name: Optional[str] = None
    avatar_url: Optional[str] = None
    description: Optional[str] = None
    is_private: Optional[bool] = None
    error: Optional[str] = None

class BleTokenResolveRequest(BaseModel):
    ble_token: str
    scanner_user_id: str
