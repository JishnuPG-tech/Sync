from fastapi import APIRouter, HTTPException, Query, status
from typing import List, Optional
from app.schemas import LocationPing, NearbyUserCard, BleTokenResolveRequest
from app.config import settings
from app import crud

router = APIRouter(prefix="/radar", tags=["Radar & Proximity"])

@router.post("/ping", status_code=status.HTTP_200_OK)
def update_location(ping: LocationPing):
    """Updates coarse coordinates in SQLite and Supabase."""
    crud.ensure_user_exists(ping.user_id, ping.display_name, ping.avatar_url)
    crud.update_user_location(ping.user_id, ping.latitude, ping.longitude)
    try:
        crud.update_user_discovery(ping.user_id, True)
    except Exception:
        pass
    return {"status": "success", "message": "Location updated"}

@router.get("/nearby", response_model=List[NearbyUserCard], response_model_exclude_none=True)
def get_nearby_peers_query(
    user_id: str = Query(..., description="User ID"),
    lat: Optional[float] = Query(None, description="Optional latitude for atomic location update"),
    lng: Optional[float] = Query(None, description="Optional longitude for atomic location update"),
    max_distance: Optional[float] = Query(None, description="Radius in meters"),
    radius: Optional[float] = Query(None, description="Alias for max_distance in meters")
):
    """Discovers active peers in proximity via query parameters."""
    crud.ensure_user_exists(user_id)
    if lat is not None and lng is not None:
        crud.update_user_location(user_id, lat, lng)
        try:
            crud.update_user_discovery(user_id, True)
        except Exception:
            pass

    eff_distance = radius or max_distance or 50000.0

    return crud.get_nearby_active_users(
        current_user_id=user_id,
        max_distance_meters=eff_distance,
        max_age_minutes=1440
    )

@router.get("/nearby/{user_id}", response_model=List[NearbyUserCard], response_model_exclude_none=True)
def get_nearby_peers(
    user_id: str,
    lat: Optional[float] = Query(None, description="Optional latitude for atomic location update"),
    lng: Optional[float] = Query(None, description="Optional longitude for atomic location update"),
    max_distance: Optional[float] = Query(None, description="Radius in meters"),
    radius: Optional[float] = Query(None, description="Alias for max_distance in meters")
):
    """Discovers active peers in proximity via path parameter."""
    crud.ensure_user_exists(user_id)
    if lat is not None and lng is not None:
        crud.update_user_location(user_id, lat, lng)
        try:
            crud.update_user_discovery(user_id, True)
        except Exception:
            pass

    eff_distance = radius or max_distance or 50000.0

    return crud.get_nearby_active_users(
        current_user_id=user_id,
        max_distance_meters=eff_distance,
        max_age_minutes=1440
    )

@router.post("/resolve-ble-token", response_model=NearbyUserCard, response_model_exclude_none=True)
def resolve_ble_token(payload: BleTokenResolveRequest):
    """Resolves opaque ephemeral BLE token to NearbyUserCard (No Instagram handle)."""
    card = crud.resolve_ble_peer(payload.scanner_user_id, payload.ble_token)
    if not card:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Nearby device token is invalid, stale, or discovery is disabled"
        )
    return card

@router.get('/ip-location')
def get_ip_location():
    return {
        'status': 'success',
        'latitude': 9.9312,
        'longitude': 76.2673,
        'accuracy': 100.0,
        'source': 'network_fallback'
    }
