from datetime import datetime, timezone
from fastapi import APIRouter, HTTPException, status
from typing import List
from app.schemas import (
    ConnectionRequestCreate,
    ConnectionRequestOut,
    ConnectionRespond,
    MutualConnectionReveal
)
from app import crud

router = APIRouter(prefix="/connections", tags=["Connections & Mutual Reveal"])

@router.post("/request", status_code=status.HTTP_201_CREATED)
def send_connection_request(payload: ConnectionRequestCreate):
    """Sends a connection request to a nearby user."""
    req, msg = crud.create_connection_request(payload.sender_id, payload.receiver_id)
    if not req:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=msg)

    return {
        "status": "success",
        "message": msg,
        "request_id": req["id"]
    }

@router.get("/pending/{user_id}", response_model=List[ConnectionRequestOut])
def get_pending_incoming_requests(user_id: str):
    """Fetches all incoming requests for the user."""
    raw_requests = crud.get_pending_requests_for_user(user_id)
    out: List[ConnectionRequestOut] = []

    for r in raw_requests:
        created_at = None
        if r.get("created_at"):
            try:
                created_at = datetime.fromisoformat(r["created_at"])
            except Exception:
                created_at = datetime.now(timezone.utc)

        out.append(
            ConnectionRequestOut(
                id=r["id"],
                sender_id=r["sender_id"],
                sender_display_name=r.get("sender_display_name", "Nearby User"),
                sender_bio=r.get("sender_bio", ""),
                sender_location=r.get("sender_location", ""),
                sender_avatar_url=r.get("sender_avatar_url"),
                sender_interests=r.get("sender_interests", []),
                status="PENDING",
                created_at=created_at or datetime.now(timezone.utc)
            )
        )
    return out

@router.post("/{request_id}/respond", status_code=status.HTTP_200_OK)
def respond_to_request(request_id: str, payload: ConnectionRespond, user_id: str):
    """Accept or Decline a connection request."""
    success, msg = crud.respond_to_connection_request(request_id, user_id, payload.action)
    if not success:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=msg)

    return {
        "status": "success",
        "action": payload.action,
        "message": msg
    }

@router.get("/mutual/{user_id}", response_model=List[MutualConnectionReveal])
def get_unlocked_connections(user_id: str):
    """THE MUTUAL REVEAL ENDPOINT: unlocks Instagram handles once mutual match occurs."""
    user = crud.get_user_by_id(user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

    return crud.get_mutual_connections(user_id)
