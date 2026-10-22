from fastapi import APIRouter, HTTPException, Query, status
from typing import List
from app.schemas import LocationPing, NearbyUserCard, BleTokenResolveRequest
from app.config import settings
from app import crud

router = APIRouter(prefix="/radar", tags=["Radar & Proximity"])

@router.post("/ping", status_code=status.HTTP_200_OK)
def update_location(ping: LocationPing):
    """Updates coarse coordinates in Supabase."""
    user = crud.get_user_by_id(ping.user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

    crud.update_user_location(ping.user_id, ping.latitude, ping.longitude)
    try:
        crud.update_user_discovery(ping.user_id, True)
    except Exception:
        pass
    return {"status": "success", "message": "Location updated"}

@router.get("/nearby/{user_id}", response_model=List[NearbyUserCard])
def get_nearby_peers(
    user_id: str,
    max_distance: float = Query(default=settings.PROXIMITY_RADIUS_METERS, description="Radius in meters")
):
    """
    CRITICAL PRIVACY ENDPOINT:
    Discovers active peers in proximity.
    Instagram usernames are NEVER returned by this endpoint.
    """
    user = crud.get_user_by_id(user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

    if not user.get("is_discovery_active", True):
        return []

    return crud.get_nearby_active_users(
        current_user_id=user_id,
        max_distance_meters=max_distance,
        max_age_minutes=settings.LOCATION_STALE_MINUTES
    )

@router.post("/resolve-ble-token", response_model=NearbyUserCard)
def resolve_ble_token(payload: BleTokenResolveRequest):
    """Resolves opaque ephemeral BLE token to NearbyUserCard (No Instagram handle)."""
    card = crud.resolve_ble_peer(payload.scanner_user_id, payload.ble_token)
    if not card:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Nearby device token is invalid, stale, or discovery is disabled"
        )
    return card
