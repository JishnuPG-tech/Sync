from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.schemas import LocationPing, NearbyUserCard, BleTokenResolveRequest
from app.config import settings
from app import crud

router = APIRouter(prefix="/radar", tags=["Radar & Proximity"])

@router.post("/ping", status_code=status.HTTP_200_OK)
def update_location(ping: LocationPing, db: Session = Depends(get_db)):
    """
    Called by mobile client when displacement occurs.
    Updates coarse coordinates.
    """
    user = crud.get_user_by_id(db, ping.user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    
    crud.update_user_location(db, ping.user_id, ping.latitude, ping.longitude)
    return {"status": "success", "message": "Location updated"}

@router.get("/nearby/{user_id}", response_model=List[NearbyUserCard])
def get_nearby_peers(
    user_id: str,
    max_distance: float = Query(default=settings.PROXIMITY_RADIUS_METERS, description="Radius in meters"),
    db: Session = Depends(get_db)
):
    """
    CRITICAL PRIVACY ENDPOINT:
    Discovers active peers in proximity.
    Returns display names, interest tags, and distance.
    Instagram usernames are NEVER returned by this endpoint.
    """
    user = crud.get_user_by_id(db, user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

    if not user.is_discovery_active:
        return []

    return crud.get_nearby_active_users(
        db,
        current_user_id=user_id,
        max_distance_meters=max_distance,
        max_age_minutes=settings.LOCATION_STALE_MINUTES
    )

@router.post("/resolve-ble-token", response_model=NearbyUserCard)
def resolve_ble_token(
    payload: BleTokenResolveRequest,
    db: Session = Depends(get_db)
):
    """
    Called when mobile Bluetooth scanner detects an opaque ephemeral BLE token.
    Resolves it to a permitted NearbyUserCard.
    Instagram handle is STRICTLY EXCLUDED.
    """
    card = crud.resolve_ble_peer(db, payload.scanner_user_id, payload.ble_token)
    if not card:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Nearby device token is invalid, stale, or discovery is disabled"
        )
    return card
